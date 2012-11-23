package com.augmentum.pushyun.service;

import static com.augmentum.pushyun.PushGlobals.displayMessage;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
import com.augmentum.pushyun.common.PushException;
import com.augmentum.pushyun.http.RegisterRequest;
import com.augmentum.pushyun.notification.NotificationBarManager;
import com.augmentum.pushyun.register.RegisterManager;
import com.augmentum.pushyun.task.BaseCallBack;
import com.augmentum.pushyun.task.PushTaskManager;

/**
 * MsgHandlerIntentService is a core base class for Services that handle asynchronous push message requests.
 *(expressed as Intents) on demand. e.g., REGISTRATION, RECEIVE, RETRY, DELETE events.
 *The request from two parts, one is from GCM server, and the other is A2DM.
  It will notify the third apps about these events.
 */
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
        this(getName("DynamicSenderIds"), PushGlobals.getInstance().getAppKey());
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
        Log.v(LOG_TAG, "Intent service name: " + name);
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
                                Log.v(LOG_TAG, "Received deleted messages notification: " + total);

                                onDeletedMessages(context, total);
                            }
                            catch (NumberFormatException e)
                            {
                                Log.e(LOG_TAG, "GCM returned invalid number of deleted messages: " + sTotal);
                            }
                        }
                    }
                    else
                    {
                        Log.e(LOG_TAG, "Received unknown special message: " + messageType);
                    }
                }
                else
                {
                    deleverToApp(context, intent);
                }
            }
            else if (action.equals("com.google.android.gcm.intent.RETRY"))
            {
                String token = intent.getStringExtra("token");
                if (!TOKEN.equals(token))
                {
                    Log.e(LOG_TAG, "Received invalid token: " + token);
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
            //TODO test the connection with A2DM
            else if (action.equals(PushA2DMService.ACTION_REGISTER))
            {
                // Register to a2dm successfully
                PushGlobals.getInstance().setRegisterInGCM(false);
                handleRegistration(context, intent);
            }
            else if(action.equals(PushA2DMService.ACTION_DELIEVERED_MSG))
            {
                deleverToApp(context, intent);
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
        Log.v(LOG_TAG, "handleRegistration: registrationId = " + registrationId + ", error = " + error + ", unregistered = "
                + unregistered);

        // Register to GCM or A2DM sucessfully
        if (registrationId != null)
        {
            RegisterManager.resetBackoff(context);

            RegisterManager.setRegistrationId(context, registrationId);
            PushGlobals.getInstance().setRegisterInGCM(true);
            
            displayMessage(context, "From GCM: device successfully registered!");
            
            onRegistered(context, registrationId, PushGlobals.getInstance().isRegisterInGCM() ? true : false);

            PushTaskManager.registerInCMSBackground(context, registrationId, new BaseCallBack()
            {
                
                @Override
                public void done(PushException paramParseException)
                {
                    if(paramParseException != null)
                    {
                        Log.v(LOG_TAG, "Have fininshed registering in CMS process");
                    }
                }
            });

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
                Log.v(LOG_TAG, "Ignoring unregister callback");
            }

            onUnregistered(context, oldRegistrationId);

            return;
        }

        Log.v("GCMBaseIntentService", "Registration error: " + error);

        if ("SERVICE_NOT_AVAILABLE".equals(error))
        {
            boolean retry = onRecoverableError(context, error);
            if (retry)
            {
                int backoffTimeMs = RegisterManager.getBackoff(context);
                int nextAttempt = backoffTimeMs / 2 + sRandom.nextInt(backoffTimeMs);

                Log.v(LOG_TAG, "Scheduling registration retry, backoff = " + nextAttempt + " (" + backoffTimeMs + ")");

                Intent retryIntent = new Intent("com.google.android.gcm.intent.RETRY");

                retryIntent.putExtra("token", TOKEN);
                PendingIntent retryPendingIntent = PendingIntent.getBroadcast(context, 0, retryIntent, 0);

                AlarmManager am = (AlarmManager)context.getSystemService("alarm");

                am.set(3, SystemClock.elapsedRealtime() + nextAttempt, retryPendingIntent);

                if (backoffTimeMs < MAX_BACKOFF_MS) RegisterManager.setBackoff(context, backoffTimeMs * 2);
            }
            else
            {
                Log.v(LOG_TAG, "Not retrying failed operation");
            }
        }
        else
        {
            //TODO how to handle the unrecoverable error process
            onError(context, error);
        }
    }

    private void deleverToApp(Context context, Intent intent)
    {
        HashMap<String, String> msgHashMap = new HashMap<String, String>();
        Iterator<String> localObject = intent.getExtras().keySet().iterator();
        while ((localObject).hasNext())
        {
          String str = localObject.next();
          msgHashMap.put(str, intent.getStringExtra(str));
        }
        if(msgHashMap.size() > 0)
        {
            NotificationBarManager.showNotification(context, msgHashMap.get("message"));
            onMessageDelivered(context, msgHashMap);
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
        Log.v(LOG_TAG, "Device unregistered");
        displayMessage(context, "From server: device successfully unregistered!");
    }
}