package com.augmentum.pushyun.register;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.augmentum.pushyun.PushA2DMManager;
import com.augmentum.pushyun.PushGlobals;
import com.augmentum.pushyun.PushManager;
import com.augmentum.pushyun.common.Logger;
import com.augmentum.pushyun.common.PushException;
import com.augmentum.pushyun.common.PushyunConfigOptions;
import com.augmentum.pushyun.http.Get;
import com.augmentum.pushyun.http.Post;
import com.augmentum.pushyun.http.response.BaseResponse;
import com.augmentum.pushyun.service.CoreMsgIntentService;
import com.augmentum.pushyun.task.HttpCallBack;

public final class RegisterManager
{
    public static final long DEFAULT_ON_SERVER_LIFESPAN_MS = 604800000L;
    private static final String BACKOFF_MS = "backoff_ms";
    private static final String GSF_PACKAGE = "com.google.android.gsf";
    private static final String PREFERENCES = "com.augmentum.pushyun";
    private static final int DEFAULT_BACKOFF_MS = 3000;
    private static final String PROPERTY_REG_ID = "regId";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String PROPERTY_ON_CMS_SERVER = "onCMSServer";
    private static final String PROPERTY_ON_SERVER_EXPIRATION_TIME = "onServerExpirationTime";
    private static final String PROPERTY_ON_SERVER_LIFESPAN = "onServerLifeSpan";
    private static Context mAppContext = PushGlobals.getAppContext();
    private static PushyunConfigOptions mPushyunConfigOptions = PushGlobals.getPushConfigOptions();

    // TODO There are too many context parameter, need to find a way to refactor this.
    public static boolean isGCMAvailable(Context context)
    {
        try
        {
            checkDevice(context);
            checkManifest(context);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public static void checkDevice(Context context)
    {
        int version = Build.VERSION.SDK_INT;
        if (version < 8) { throw new UnsupportedOperationException("Device must be at least API Level 8 (instead of " + version + ")"); }

        PackageManager packageManager = context.getPackageManager();
        try
        {
            packageManager.getPackageInfo(GSF_PACKAGE, 0);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            throw new UnsupportedOperationException("Device does not have package com.google.android.gsf");
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void checkManifest(Context context)
    {
        PackageManager packageManager = context.getPackageManager();
        String packageName = context.getPackageName();
        String permissionName = packageName + ".permission.C2D_MESSAGE";
        try
        {
            packageManager.getPermissionInfo(permissionName, 4096);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            throw new IllegalStateException("Application does not define permission " + permissionName);
        }

        PackageInfo receiversInfo;
        try
        {
            receiversInfo = packageManager.getPackageInfo(context.getPackageName(), 2);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            throw new IllegalStateException("Could not get receivers for package " + packageName);
        }

        ActivityInfo[] receivers = receiversInfo.receivers;
        if ((receivers == null) || (receivers.length == 0)) { throw new IllegalStateException("No receiver for package " + packageName); }

        Logger.verbose(Logger.GCM_LOG_TAG, "number of receivers for " + packageName + ": " + receivers.length);

        Set allowedReceivers = new HashSet();
        for (ActivityInfo receiver : receivers)
        {
            if (!"com.google.android.c2dm.permission.SEND".equals(receiver.permission)) continue;
            allowedReceivers.add(receiver.name);
        }

        if (allowedReceivers.isEmpty()) { throw new IllegalStateException(
                "No receiver allowed to receive com.google.android.c2dm.permission.SEND"); }

        checkReceiver(context, allowedReceivers, "com.google.android.c2dm.intent.REGISTRATION");

        checkReceiver(context, allowedReceivers, "com.google.android.c2dm.intent.RECEIVE");
    }

    private static void checkReceiver(Context context, Set<String> allowedReceivers, String action)
    {
        PackageManager pm = context.getPackageManager();
        String packageName = context.getPackageName();
        Intent intent = new Intent(action);
        intent.setPackage(packageName);
        List<ResolveInfo> receivers = pm.queryBroadcastReceivers(intent, 32);

        if (receivers.isEmpty()) { throw new IllegalStateException("No receivers for action " + action); }

        Logger.verbose(Logger.GCM_LOG_TAG, "Found " + receivers.size() + " receivers for action " + action);

        for (ResolveInfo receiver : receivers)
        {
            String name = receiver.activityInfo.name;
            if (!allowedReceivers.contains(name))
                throw new IllegalStateException("Receiver " + name + " is not set with permission "
                        + "com.google.android.c2dm.permission.SEND");
        }
    }

    /**
     * With GCM developer mGCMDeveloperId and GSF(google service framework), request reg_ID,
     * register or unregister from GCM with Intent("com.google.android.c2dm.intent.REGISTER") or
     * Intent("com.google.android.c2dm.intent.UNREGISTER").
     */
    public static void registerInGCM(Context context, String senderIds)
    {
        resetBackoff(context);
        internalRegister(context, senderIds);
    }

    public static void internalRegister(Context context, String senderIds)
    {
        String flatSenderIds = getFlatSenderIds(senderIds);

        Intent intent = new Intent("com.google.android.c2dm.intent.REGISTER");
        intent.setPackage(GSF_PACKAGE);
        intent.putExtra("app", PendingIntent.getBroadcast(context, 0, new Intent(), 0));
        intent.putExtra("sender", flatSenderIds);
        context.startService(intent);
        Logger.verbose(Logger.GCM_LOG_TAG, "Broadcast register to GCM : Registering app " + context.getPackageName() + " of senders "
                + flatSenderIds);
    }

    public static String getFlatSenderIds(String... senderIds)
    {
        if ((senderIds == null) || (senderIds.length == 0)) { throw new IllegalArgumentException("No senderIds"); }
        StringBuilder builder = new StringBuilder(senderIds[0]);
        for (int i = 1; i < senderIds.length; ++i)
        {
            builder.append(',').append(senderIds[i]);
        }
        return builder.toString();
    }

    public static void unregister(Context context)
    {
        resetBackoff(context);
        internalUnregister(context);
    }

    public static synchronized void onDestroy(Context context)
    {
        // Release resource TODO
    }

    public static void internalUnregister(Context context)
    {
        Intent intent = new Intent("com.google.android.c2dm.intent.UNREGISTER");
        intent.setPackage(GSF_PACKAGE);
        intent.putExtra("app", PendingIntent.getBroadcast(context, 0, new Intent(), 0));
        context.startService(intent);
        Logger.verbose(Logger.GCM_LOG_TAG, "Broadcast unregister to GCM : Unregistering app " + context.getPackageName());
    }

    public static String getRegistrationId()
    {
        SharedPreferences prefs = getPushyunReferences(mAppContext);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");

        int oldVersion = prefs.getInt(PROPERTY_APP_VERSION, -2147483648);
        int newVersion = getAppVersion(mAppContext);
        if ((oldVersion != -2147483648) && (oldVersion != newVersion))
        {
            Logger.verbose(Logger.GCM_LOG_TAG, "App version changed from " + oldVersion + " to " + newVersion
                    + "; resetting registration id");

            clearRegistrationId(mAppContext);
            registrationId = "";
        }
        return registrationId;
    }

    public static boolean isRegisteredInGCMOrA2DM()
    {
        return getRegistrationId().length() > 0;
    }

    public static String clearRegistrationId(Context context)
    {
        return setRegistrationId(context, "");
    }

    public static String setRegistrationId(Context context, String regId)
    {
        SharedPreferences prefs = getPushyunReferences(context);
        String oldRegistrationId = prefs.getString(PROPERTY_REG_ID, "");
        int appVersion = getAppVersion(context);
        Log.v("GCMRegistrar", "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
        return oldRegistrationId;
    }

    public static void setRegisteredOnCMSServer(Context context, boolean flag)
    {
        SharedPreferences prefs = getPushyunReferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PROPERTY_ON_CMS_SERVER, flag);

        long lifespan = getRegisterOnServerLifespan(context);
        long expirationTime = System.currentTimeMillis() + lifespan;
        editor.putLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, expirationTime);
        editor.commit();

        Logger.verbose(Logger.GCM_LOG_TAG, "Setting registeredOnServer status as " + flag + " until " + new Timestamp(expirationTime));
    }

    public static boolean isRegisteredOnCMSServer()
    {
        SharedPreferences prefs = getPushyunReferences(mAppContext);
        boolean isRegistered = prefs.getBoolean(PROPERTY_ON_CMS_SERVER, false);
        if (isRegistered)
        {
            long expirationTime = prefs.getLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, -1L);

            if (System.currentTimeMillis() > expirationTime)
            {
                Log.v("GCMRegistrar", "flag expired on: " + new Timestamp(expirationTime));
                return false;
            }
        }
        Logger.verbose(Logger.GCM_LOG_TAG, "Is registered on server: " + isRegistered);
        return isRegistered;
    }

    public static long getRegisterOnServerLifespan(Context context)
    {
        SharedPreferences prefs = getPushyunReferences(context);
        long lifespan = prefs.getLong(PROPERTY_ON_SERVER_LIFESPAN, 604800000L);

        return lifespan;
    }

    public static void setRegisterOnServerLifespan(Context context, long lifespan)
    {
        SharedPreferences prefs = getPushyunReferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(PROPERTY_ON_SERVER_LIFESPAN, lifespan);
        editor.commit();
    }

    private static int getAppVersion(Context context)
    {
        try
        {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);

            return packageInfo.versionCode;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            throw new RuntimeException("Coult not get package name: " + e);
        }
    }

    public static void resetBackoff(Context context)
    {
        setBackoff(context, DEFAULT_BACKOFF_MS);
        Logger.verbose(Logger.GCM_LOG_TAG, "resetting backoff for " + context.getPackageName());
    }

    public static int getBackoff(Context context)
    {
        SharedPreferences prefs = getPushyunReferences(context);
        return prefs.getInt(BACKOFF_MS, DEFAULT_BACKOFF_MS);
    }

    public static void setBackoff(Context context, int backoff)
    {
        SharedPreferences prefs = getPushyunReferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(BACKOFF_MS, backoff);
        editor.commit();
    }

    private static SharedPreferences getPushyunReferences(Context context)
    {
        return context.getSharedPreferences(PREFERENCES, 0);
    }

    private RegisterManager()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Do the registration work, communication with GCM, A2DM，CMS server，and should not processed in
     * the UI thread.
     */
    public static void doRegistrationTask()
    {
        if (isRegisteredInGCMOrA2DM())
        {
            if (!isRegisteredOnCMSServer())
            {
                registerToCMS();
            }
            // GCM is prior to A2DM, init A2DM connection when the GCM is unavailable
            else if (!(mPushyunConfigOptions.isGCMEnabled() && isGCMAvailable(mAppContext)))
            {
                PushA2DMManager.initSoildA2DMConnection(mAppContext);
            }
        }
        else
        {
            if (mPushyunConfigOptions.isGCMEnabled())
            {
                registerWithGCM();
            }
            else
            {
                registerToA2DM();
            }
        }
    }

    /**
     * Check GCM service is available or not, Prior to use GCM service.
     */
    private static void registerWithGCM()
    {
        if (isGCMAvailable(mAppContext))
        {
            PushGlobals.getInstance().setGCMAvailabe(true);
            registerInGCM(mAppContext, mPushyunConfigOptions.getGCMAppKey());
        }
        else
        {
            registerToA2DM();
        }
    }

    /**
     * 
     * Generate a unique value for registering in A2DM according to application and device. It's
     * unique identify for A2DM
     * 
     * @param context
     * @return Unique value
     */
    private static String generateUDIDValue(Context context)
    {
        final TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        final String tmDevice, tmSerial, bluetoothAdd, androidId;

        // IMID (International Mobile Equipment Identity) Tab doesn't have it
        tmDevice = "" + tm.getDeviceId();

        // The serial number of the SIM, if applicable. Return null if it is unavailable.
        tmSerial = "" + tm.getSimSerialNumber();

        bluetoothAdd = "" + bluetoothAdapter.getAddress();

        /**
         * It's known to be null sometimes, it's documented as "can change upon factory reset". Use
         * at your own risk, and it can be easily changed on a rooted phone.
         */
        androidId = "" + Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);

        // Universally Unique Identifier
        UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode() | bluetoothAdd.hashCode());

        return deviceUuid.toString();
    }

    /**
     * The A2DM server will be instead of GCM server, when the GCM server is not available. The
     * process is simulator with GCM, and will keep alive and stable connection with A2DM.
     */
    public static void registerToA2DM()
    {
        List<BasicNameValuePair> nameValuePairs = new ArrayList<BasicNameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("udid", generateUDIDValue(mAppContext)));
        nameValuePairs.add(new BasicNameValuePair("app", mPushyunConfigOptions.getA2DMAppKey()));

        Get get = new Get(PushGlobals.A2DM_SERVER_REGISTER_URL, nameValuePairs);

        PushManager.executeHttpRequest(get, PushGlobals.GET_METHOD, new HttpCallBack()
        {
            @Override
            public void done(BaseResponse respone, PushException e)
            {
                if (e != null && respone != null)
                {
                    JSONObject jsonData = respone.getJSONData();
                    if (respone.isStatusOk() && jsonData != null)
                    {
                        try
                        {
                            String token = jsonData.getString("token");
                            Intent intent = new Intent(PushGlobals.A2DM_REGISTER_SUCCESS_ACTION);
                            intent.putExtra("registration_id", token);

                            CoreMsgIntentService.runIntentInService(mAppContext, intent, mPushyunConfigOptions.getAppIntentServicePath());

                            // TODO need to check the fail reason for receiver
                            // PushGlobals.sendPushBroadcast(PushService.this,
                            // PushGlobals.A2DM_REGISTER_SUCCESS_ACTION, token);
                        }
                        catch (JSONException exception)
                        {
                            exception.printStackTrace();
                        }
                    }
                }
                else
                {
                    // Handle the exception
                }
            }
        });

    }

    /**
     * Register this account/device pair within the CMS server.
     * 
     * @return whether the registration succeeded or not.
     */
    public static void registerToCMS()
    {
        List<BasicNameValuePair> nameValuePairs = new ArrayList<BasicNameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("appkey", "com.push.test"));
        nameValuePairs.add(new BasicNameValuePair("token", getRegistrationId()));
        nameValuePairs.add(new BasicNameValuePair("name", "Test"));
        nameValuePairs.add(new BasicNameValuePair("version", "1"));

        Post post = new Post(PushGlobals.CMS_SERVER_REGISTER_URL, nameValuePairs);

        PushManager.executeHttpRequest(post, PushGlobals.POST_METHOD, new HttpCallBack()
        {
            @Override
            public void done(BaseResponse respone, PushException e)
            {
                if (e != null && respone != null)
                {
                    if (respone.isStatusOk())
                    {
                        // Register in CMS server successfully
                        setRegisteredOnCMSServer(mAppContext, true);
                        // PushA2DMManager.initSoildA2DMConnection(mAppContext);

                        PushGlobals.sendPushBroadcast(mAppContext, PushGlobals.DISPLAY_MESSAGE_ACTION,
                                "From CMS Server: successfully added device!");
                    }
                    // Register in CMS failed, May be need to unregister in GCM or A2DM, google just
                    // unregister in GCM
                    else
                    {
                        // TODO unregister(context);
                    }
                }
                else
                {
                    // Handle the exception
                }
            }
        });
    }
}
