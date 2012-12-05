package com.augmentum.pushyun.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.augmentum.pushyun.PushGlobals;
import com.augmentum.pushyun.broadcast.CoreBroadcastReceiver;
import com.augmentum.pushyun.common.Logger;
import com.augmentum.pushyun.register.RegisterManager;

/**
 *  TODO 
 *  The background long alive service of the Pushyun, manage the Android system resources.
 *  Log the status of service currently.
 *
 */
public class PushService extends Service
{

    private static final String CHECK_REGISTERATION_ACTION = "com.augmentum.pushyun.service.REGISTERATION";
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

        if (intent.getAction().equals(CHECK_REGISTERATION_ACTION) == true)
        {
            RegisterManager.doRegistrationTask();
        }

        Logger.verbose(Logger.SERVICE_LOG_TAG, "PushService onStart with action = " + intent.getAction());
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
     * Start pushyun service. Load airshipconfig.properties, register to server and retrieve message
     * from server according to the configuration options
     * 
     * @param appContext Application context
     */
    public static void launchPushyunService(Context appContext)
    {
        PushGlobals.setAppContext(appContext);
        PushGlobals.getPushConfigOptions().loadPushyunConfigOptions(appContext);

        Intent i = new Intent(appContext, PushService.class);
        i.setAction(CHECK_REGISTERATION_ACTION);
        appContext.startService(i);
    }

    /**
     * Pushyun service should be as a long alive service, the alive status will be checked by
     * {@link CoreBroadcastReceiver} according to "android.intent.action.BOOT_COMPLETED" &&
     * "android.intent.action.USER_PRESENT" events action.
     * 
     * @param context
     */
    public static void launchPushyunServiceIfRequired(Context context)
    {
        if (PushGlobals.getAppContext() == null)
        {
            launchPushyunService(context);
        }
    }
}
