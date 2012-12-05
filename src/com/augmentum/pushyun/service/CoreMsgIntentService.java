package com.augmentum.pushyun.service;

import static com.augmentum.pushyun.PushGlobals.DISPLAY_MESSAGE_ACTION;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.SystemClock;

import com.augmentum.pushyun.PushA2DMManager;
import com.augmentum.pushyun.PushGlobals;
import com.augmentum.pushyun.broadcast.CoreBroadcastReceiver;
import com.augmentum.pushyun.common.Logger;
import com.augmentum.pushyun.notification.PushNotificationManager;
import com.augmentum.pushyun.register.RegisterManager;

/**
 * CoreHandlerIntentService.java is a core base class for Services that handle asynchronous push
 * message requests. (expressed as Intents) on demand. e.g., REGISTRATION, RECEIVE, RETRY, DELETE
 * events. The request from two parts, one is from GCM server, and the other is A2DM. It will notify
 * the third apps about these events.
 */
public abstract class CoreMsgIntentService extends IntentService
{
    private static final String WAKELOCK_KEY = "GCM_LIB";
    private static final int MAX_BACKOFF_MS = (int)TimeUnit.SECONDS.toMillis(3600L);

    private static final Object LOCK = CoreMsgIntentService.class;
    private static PowerManager.WakeLock mWakeLock;
    private static final Random sRandom = new Random();
    private static final String TOKEN = Long.toBinaryString(sRandom.nextLong());

    private static boolean mRegisterInGCM = true;

    protected CoreMsgIntentService()
    {
        this("CoreMsgIntentService");
    }

    protected CoreMsgIntentService(String name)
    {
        super(name);
    }

    /**
     * Core message business handler
     */
    @Override
    public final void onHandleIntent(Intent intent)
    {
        try
        {
            Context context = getApplicationContext();
            String action = intent.getAction();
            if (action.equals("com.google.android.c2dm.intent.REGISTRATION"))
            {
                mRegisterInGCM = true;
                handleRegistration(context, intent);
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
                                Logger.verbose(Logger.SERVICE_LOG_TAG, "Received deleted messages notification: " + total);

                                onDeletedMessages(context, total);
                            }
                            catch (NumberFormatException e)
                            {
                                Logger.error(Logger.SERVICE_LOG_TAG, "GCM returned invalid number of deleted messages: " + sTotal);
                            }
                        }
                    }
                    else
                    {
                        Logger.verbose(Logger.SERVICE_LOG_TAG, "Received unknown special message: " + messageType);
                    }
                }
                else
                {
                    deleverMsgToApp(context, intent);
                }
            }
            else if (action.equals("com.augmentum.pushyun.service.intent.RETRY"))
            {
                String token = intent.getStringExtra("token");
                if (!TOKEN.equals(token))
                {
                    Logger.verbose(Logger.SERVICE_LOG_TAG, "Received invalid token: " + token);
                    return;
                }

                if (RegisterManager.isRegisteredInGCMOrA2DM())
                {
                    RegisterManager.internalUnregister(context);
                }
                else
                {
                    RegisterManager.internalRegister(context, PushGlobals.getPushConfigOptions().getGCMAppKey());
                }

            }
            // TODO test the connection with A2DM
            else if (action.equals(PushGlobals.A2DM_REGISTER_SUCCESS_ACTION))
            {
                // Register to a2dm successfully
                mRegisterInGCM = false;
                handleRegistration(context, intent);
            }
            else if (action.equals(PushA2DMManager.ACTION_DELIEVERED_MSG))
            {
                deleverMsgToApp(context, intent);
            }
        }
        finally
        {
            synchronized (LOCK)
            {
                if (mWakeLock != null)
                {
                    Logger.verbose(Logger.SERVICE_LOG_TAG, "Releasing wakelock");
                    mWakeLock.release();
                }
                else
                {
                    Logger.verbose(Logger.SERVICE_LOG_TAG, "Wakelock reference is null");
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

        Logger.verbose(Logger.SERVICE_LOG_TAG, "Acquiring wakelock");
        mWakeLock.acquire();
        intent.setClassName(context, className);
        context.startService(intent);
    }

    /**
     * Process message from server
     * 
     * @param context
     * @param intent
     */
    private void handleRegistration(Context context, Intent intent)
    {
        String registrationId = intent.getStringExtra("registration_id");
        String error = intent.getStringExtra("error");
        String unregistered = intent.getStringExtra("unregistered");
        Logger.verbose(Logger.SERVICE_LOG_TAG, "handleRegistration: registrationId = " + registrationId + ", error = " + error
                + ", unregistered = " + unregistered);

        PushNotificationManager.getInstance().deliverPushNotification(PushGlobals.getAppName(), "Test message");

        // Register to GCM or A2DM successfully
        if (registrationId != null)
        {
            RegisterManager.resetBackoff(context);

            RegisterManager.setRegistrationId(context, registrationId);

            PushGlobals.getInstance().setRegisterInGCM(mRegisterInGCM);

            String platform = mRegisterInGCM ? "GCM" : "A2DM";
            String msg = "From " + platform + " : Device successfully registered! token=" + registrationId;
            PushGlobals.sendPushBroadcast(context, DISPLAY_MESSAGE_ACTION, msg);

            onRegistered(context, registrationId, PushGlobals.getInstance().isRegisterInGCM() ? true : false);

            PushNotificationManager.getInstance().deliverPushNotification(PushGlobals.getAppName(), msg);

            RegisterManager.registerToCMS();

            return;
        }

        if (unregistered != null)
        {
            RegisterManager.resetBackoff(context);
            String oldRegistrationId = RegisterManager.clearRegistrationId(context);

            // Unregister api TODO
            if (RegisterManager.isRegisteredOnCMSServer())
            {
                // At this point all attempts to register with the app
                // server failed, so we need to unregister the device
                // from GCM - the app will try to register again when
                // it is restarted. Note that GCM will send an
                // unregistered callback upon completion, but
                // GCMIntentService.onUnregistered() will ignore it.
            }
            else
            {
                // This callback results from the call to unregister made on
                // ServerUtilities when the registration to the server failed.
                Logger.verbose(Logger.SERVICE_LOG_TAG, "Ignoring unregister callback");
            }

            onUnregistered(context, oldRegistrationId);

            return;
        }

        if ("SERVICE_NOT_AVAILABLE".equals(error))
        {
            int backoffTimeMs = RegisterManager.getBackoff(context);
            int nextAttempt = backoffTimeMs / 2 + sRandom.nextInt(backoffTimeMs);

            Logger.verbose(Logger.SERVICE_LOG_TAG, "Scheduling registration retry, backoff = " + nextAttempt + " (" + backoffTimeMs + ")");

            Intent retryIntent = new Intent(context, CoreBroadcastReceiver.class);
            retryIntent.setAction("com.augmentum.pushyun.service.intent.RETRY");

            retryIntent.putExtra("token", TOKEN);
            PendingIntent retryPendingIntent = PendingIntent.getBroadcast(context, 0, retryIntent, 0);
            AlarmManager am = (AlarmManager)context.getSystemService("alarm");
            am.set(3, SystemClock.elapsedRealtime() + nextAttempt, retryPendingIntent);
            if (backoffTimeMs < MAX_BACKOFF_MS) RegisterManager.setBackoff(context, backoffTimeMs * 2);

            onRecoverableError(context, error);
        }
        else if (error != null)
        {
            // unrecoverable error list : SERVICE_NOT_AVAILABLE, ACCOUNT_MISSING,
            // AUTHENTICATION_FAILED,
            // TOO_MANY_REGISTRATIONS, INVALID_SENDER, PHONE_REGISTRATION_ERROR

            RegisterManager.registerInGCM(context, PushGlobals.getInstance().getAppKey());
            Logger.verbose(Logger.SERVICE_LOG_TAG, "Register to GCM failed, ");
            onError(context, "Register to GCM failed unrecoverable error: " + error + " try to register to A2DM");
        }
    }

    private void deleverMsgToApp(Context context, Intent intent)
    {
        HashMap<String, String> msgHashMap = new HashMap<String, String>();
        Iterator<String> localObject = intent.getExtras().keySet().iterator();
        while ((localObject).hasNext())
        {
            String str = localObject.next();
            msgHashMap.put(str, intent.getStringExtra(str));
        }
        if (msgHashMap.size() > 0)
        {
            // PushNotificationManager.showNotification(context, msgHashMap.get("message"));
            PushNotificationManager.getInstance().deliverPushNotification(PushGlobals.getAppName(), msgHashMap.get("message"));
            onMessageDelivered(context, msgHashMap);
        }
    }

    protected boolean onRecoverableError(Context context, String errorId)
    {
        Logger.verbose(Logger.SERVICE_LOG_TAG, "Received recoverable error: " + errorId);
        PushGlobals.sendPushBroadcast(context, DISPLAY_MESSAGE_ACTION, "From GCM: Recoverable error " + errorId);
        return true;
    }

    protected void onDeletedMessages(Context context, int total)
    {
        Logger.verbose(Logger.SERVICE_LOG_TAG, "Received deleted messages notification");
        String message = "From GCM: server deleted %1$d pending messages!";
        PushGlobals.sendPushBroadcast(context, DISPLAY_MESSAGE_ACTION, message);
    }

    /**
     * Process delivered message according to your key/value string
     * 
     * @param context Content context
     * @param msgMap Including push message data.
     */
    protected abstract void onMessageDelivered(Context context, HashMap<String, String> msgMap);

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
        Logger.verbose(Logger.SERVICE_LOG_TAG, "Device unregistered");
        PushGlobals.sendPushBroadcast(context, DISPLAY_MESSAGE_ACTION, "From server: device successfully unregistered!");
    }
}