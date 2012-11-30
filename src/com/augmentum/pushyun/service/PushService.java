package com.augmentum.pushyun.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.augmentum.pushyun.PushGlobals;
import com.augmentum.pushyun.register.RegisterManager;

public class PushService extends Service
{
    private static final String LOG_TAG = "Pushervice";

    private static final String ACTION_REGISTER = "com.augmentum.pushyun.service.REGISTER";

    // Properties can configured in assets and PushUser refine it TODO
    private static String mAppKey = "";
    private static String mToken = "";
    private static String mName = "";
    private static String mVersion = "";
    private static String mAppMsgIntentServiceClassPath = "";
    private static String mNotificationBarStylePath = "";
    private static Context mAppContext = null;

    private static PushGlobals mPushGlobals = PushGlobals.getInstance();

    @Override
    public void onCreate()
    {
        super.onCreate();
        mPushGlobals.registerDebugMsgReceiver(this);
    }

    /**
     * Handle service request.
     */
    @Override
    public void onStart(Intent intent, int startId)
    {
        super.onStart(intent, startId);

        if (intent.getAction().equals(ACTION_REGISTER) == true)
        {
            RegisterManager.doRegistrationTask(this);
        }

        Log.v(LOG_TAG, "PushService onStart with action = " + intent.getAction());
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        throw new IllegalArgumentException("You cannot bind directly to the PushService.");
    }

    /**
     * Release android system resource
     */
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mPushGlobals.unRegisterDebugMsgReceiver(this);
        RegisterManager.onDestroy(this);
    }

    /**
     * TODO refine the application process.
     * @param context Application context
     * @param intent
     */
    public static void startToLoad(Context context, Intent intent)
    {
        mAppContext = context;
        Intent i = new Intent(context, PushService.class);
        if (intent != null)
        {
            mPushGlobals.setAppKey(intent.getStringExtra("app_key"));
            mPushGlobals.setAppMsgIntentServiceClassPath(intent.getStringExtra("app_service_path"));
            mPushGlobals.setGCMEnabled(intent.getBooleanExtra("gcm_enabled", true));
        }
        i.setAction(ACTION_REGISTER);
        context.startService(i);
    }
}
