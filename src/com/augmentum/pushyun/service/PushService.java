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
            RegisterManager.doRegistrationTask();
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
     * TODO Refines the application process.
     * @param appContext Application context
     * @param intent
     */
    public static void launchPushyunService(Context appContext, Intent intent)
    {
        if (intent != null)
        {
            PushGlobals.getPushConfigOptions().loadPushyunConfigOptions(intent);
        }
        launchPushyunService(appContext);
    }
    
    /**
     * Start pushyun service. Load airshipconfig.properties, 
     * register to server and retrieve message from server according to the configuration options
     * @param appContext Application context
     */
    public static void launchPushyunService(Context appContext)
    {
        PushGlobals.setAppContext(appContext);
        PushGlobals.getPushConfigOptions().loadPushyunConfigOptions(appContext);
        
        Intent i = new Intent(appContext, PushService.class);
        i.setAction(ACTION_REGISTER);
        appContext.startService(i);
    }
}
