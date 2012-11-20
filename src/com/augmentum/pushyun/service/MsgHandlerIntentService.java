package com.augmentum.pushyun.service;

import static com.augmentum.pushyun.PushGlobals.displayMessage;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import com.augmentum.pushyun.PushGlobals;
import com.augmentum.pushyun.http.RegisterRequest;
import com.augmentum.pushyun.manager.RegisterManager;

public abstract class MsgHandlerIntentService extends IntentService
{
    public static final String LOG_TAG = "MsgHandlerIntentService";
    private static final String WAKELOCK_KEY = "GCM_LIB";
    private static final Object LOCK = MsgHandlerIntentService.class;
    private final String[] mSenderIds;
    private static PowerManager.WakeLock mWakeLock;
    private static int mCounter = 0;

    private static final Random sRandom = new Random();

    private static final int MAX_BACKOFF_MS = (int)TimeUnit.SECONDS.toMillis(3600L);

    private static final String TOKEN = Long.toBinaryString(sRandom.nextLong());

    protected MsgHandlerIntentService()
    {
        this(getName("DynamicSenderIds"), null);
    }

    protected MsgHandlerIntentService(String... senderIds)
    {
        this(getName(senderIds), senderIds);
    }

    private MsgHandlerIntentService(String name, String[] senderIds)
    {
        super(name);
        this.mSenderIds = senderIds;
    }

    private static String getName(String senderId)
    {
        String name = "GCMIntentService-" + senderId + "-" + ++mCounter;
        Log.v("GCMBaseIntentService", "Intent service name: " + name);
        return name;
    }

    private static String getName(String[] senderIds)
    {
        String flatSenderIds = RegisterManager.getFlatSenderIds(senderIds);
        return getName(flatSenderIds);
    }

    protected String[] getSenderIds(Context context)
    {
        if (this.mSenderIds == null) { throw new IllegalStateException("sender id not set on constructor"); }
        return this.mSenderIds;
    }

    public final void onHandleIntent(Intent intent)
    {
        try
        {
            Context context = getApplicationContext();
            String action = intent.getAction();
            if (action.equals("com.google.android.c2dm.intent.REGISTRATION"))
            {
                handleRegistration(context, intent);
                RegisterManager.setRetryBroadcastReceiver(context);
            }
            else if (action.equals("com.google.android.c2dm.intent.RECEIVE"))
            {
                String messageType = intent.getStringExtra("message_type");

                if (messageType != null)
                {
                    if (messageType.equals("deleted_messages"))
                    {
                        String sTotal = intent.getStringExtra("total_deleted");

                        if (sTotal != null)
                        {
                            try
                            {
                                int total = Integer.parseInt(sTotal);
                                Log.v("GCMBaseIntentService", "Received deleted messages notification: " + total);

                                onDeletedMessages(context, total);
                            }
                            catch (NumberFormatException e)
                            {
                                Log.e("GCMBaseIntentService", "GCM returned invalid number of deleted messages: " + sTotal);
                            }
                        }
                    }
                    else
                    {
                        Log.e("GCMBaseIntentService", "Received unknown special message: " + messageType);
                    }
                }
                else
                {
                    onMessageDelivered(context, intent);
                }

            }
            else if (action.equals("com.google.android.gcm.intent.RETRY"))
            {
                String token = intent.getStringExtra("token");
                if (!TOKEN.equals(token))
                {
                    Log.e("GCMBaseIntentService", "Received invalid token: " + token);
                    return;
                }

                if (RegisterManager.isRegistered(context))
                {
                    RegisterManager.internalUnregister(context);
                }
                else
                {
                    String[] senderIds = getSenderIds(context);
                    RegisterManager.internalRegister(context, senderIds);
                }

            }
            else if (action.equals(PushA2DMService.ACTION_REGISTER))
            {
                // Register to a2dm successfully
                PushGlobals.getInstance().setRegisterInGCM(false);
                handleRegistration(context, intent);
            }

        }
        finally
        {
            synchronized (LOCK)
            {
                if (mWakeLock != null)
                {
                    Log.v("GCMBaseIntentService", "Releasing wakelock");
                    mWakeLock.release();
                }
                else
                {
                    Log.e("GCMBaseIntentService", "Wakelock reference is null");
                }
            }
        }
    }

    public static void runIntentInService(Context context, Intent intent, String className)
    {
        synchronized (LOCK)
        {
            if (mWakeLock == null)
            {
                PowerManager pm = (PowerManager)context.getSystemService("power");

                mWakeLock = pm.newWakeLock(1, WAKELOCK_KEY);
            }
        }

        Log.v("GCMBaseIntentService", "Acquiring wakelock");
        mWakeLock.acquire();
        intent.setClassName(context, className);
        context.startService(intent);
    }

    private void handleRegistration(Context context, Intent intent)
    {
        String registrationId = intent.getStringExtra("registration_id");
        String error = intent.getStringExtra("error");
        String unregistered = intent.getStringExtra("unregistered");
        Log.d("GCMBaseIntentService", "handleRegistration: registrationId = " + registrationId + ", error = " + error + ", unregistered = "
                + unregistered);

        // Register to GCM or A2DM sucessfully
        if (registrationId != null)
        {
            RegisterManager.resetBackoff(context);

            RegisterManager.setRegistrationId(context, registrationId);
            PushGlobals.getInstance().setRegisterInGCM(true);

            onRegistered(context, registrationId, PushGlobals.getInstance().isRegisterInGCM() ? true : false);
            
            RegisterRequest.registerCMSServer(context, registrationId);


            return;
        }

        if (unregistered != null)
        {
            RegisterManager.resetBackoff(context);
            String oldRegistrationId = RegisterManager.clearRegistrationId(context);

            if (RegisterManager.isRegisteredOnCMSServer(context))
            {
                RegisterRequest.unregisterCMSServer(context, registrationId);
            }
            else
            {
                // This callback results from the call to unregister made on
                // ServerUtilities when the registration to the server failed.
                Log.i(LOG_TAG, "Ignoring unregister callback");
            }

            onUnregistered(context, oldRegistrationId);

            return;
        }

        Log.d("GCMBaseIntentService", "Registration error: " + error);

        if ("SERVICE_NOT_AVAILABLE".equals(error))
        {
            boolean retry = onRecoverableError(context, error);
            if (retry)
            {
                int backoffTimeMs = RegisterManager.getBackoff(context);
                int nextAttempt = backoffTimeMs / 2 + sRandom.nextInt(backoffTimeMs);

                Log.d("GCMBaseIntentService", "Scheduling registration retry, backoff = " + nextAttempt + " (" + backoffTimeMs + ")");

                Intent retryIntent = new Intent("com.google.android.gcm.intent.RETRY");

                retryIntent.putExtra("token", TOKEN);
                PendingIntent retryPendingIntent = PendingIntent.getBroadcast(context, 0, retryIntent, 0);

                AlarmManager am = (AlarmManager)context.getSystemService("alarm");

                am.set(3, SystemClock.elapsedRealtime() + nextAttempt, retryPendingIntent);

                if (backoffTimeMs < MAX_BACKOFF_MS) RegisterManager.setBackoff(context, backoffTimeMs * 2);
            }
            else
            {
                Log.d("GCMBaseIntentService", "Not retrying failed operation");
            }
        }
        else
        {
            onError(context, error);
        }
    }

    protected boolean onRecoverableError(Context context, String errorId)
    {
        Log.v(LOG_TAG, "Received recoverable error: " + errorId);
        displayMessage(context, "From GCM: recoverable error " + errorId);
        return true;
    }

    protected void onDeletedMessages(Context context, int total)
    {
        Log.v(LOG_TAG, "Received deleted messages notification");
        String message = "From GCM: server deleted %1$d pending messages!";
        displayMessage(context, message);
    }

    /**
     * Process delivered message, if your key/value is a JSON string, just extract it and parse it
     * using JSONObject String json_info = intent.getExtras().getString("data"); JSONObject jsonObj
     * = new JSONObject(data);
     * 
     * @param context Content context
     * @param msgIntent Including push message data.
     */
    protected abstract void onMessageDelivered(Context context, Intent msgIntent);

    /**
     * TODO Error type details
     * 
     * @param context
     * @param error Error message
     */
    protected abstract void onError(Context context, String error);

    /**
     * 
     * @param context Content context
     * @param regId Token, CMS server will send message to device according the token.
     * @param inGCM Register to GCM or A2DM server
     */
    protected abstract void onRegistered(Context context, String regId, boolean inGCM);

    // protected abstract void onUnregistered(Context context, String paramString);

    // TODO whether need this interface
    private void onUnregistered(Context context, String regId)
    {
        Log.v(LOG_TAG, "Device unregistered");
        displayMessage(context, "From server: device successfully unregistered!");
    }
}