package com.augmentum.pushyun.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;

import com.augmentum.pushyun.PushGlobals;
import com.augmentum.pushyun.http.RegisterRequest;
import com.augmentum.pushyun.manager.RegisterManager;

public class PushService extends Service
{
    private static final String LOG_TAG = "Pushervice";
    
    private static final String ACTION_REGISTER = "com.augmentum.pushyun.service.REGISTER";

    //Properties can configured in assets and PushUser refine it TODO
    private static String mAppKey = "";
    private static String mToken = "";
    private static String mName = "";
    private static String mVersion = "";
    private static String mAppMsgIntentServiceClassPath = "";
    private static String mNotificationBarStylePath = "";
    
    // GCM developer project id
    private static String mGCMDeveloperId = PushGlobals.SENDER_ID;

    private static PushGlobals mPushGlobals = PushGlobals.getInstance();
    private static AsyncTask<Void, Void, Void> mRegisterCMSTask = null;
    private static Context mAppContext = null;

    // private static boolean mCheckedGCM = false;
    // private static boolean mGCMAvaiable = false;

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
            if(mPushGlobals.isGCMEnabled())
            {
                checkGCMStatus();
            }
            else
            {
                registerWithA2DM();
            }
        }
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
        if (mRegisterCMSTask != null)
        {
            mRegisterCMSTask.cancel(true);
        }
        mPushGlobals.unRegisterDebugMsgReceiver(this);
        RegisterManager.onDestroy(this);
    }

    
    public static void register(Context context, Intent intent)
    {
        mAppContext = context;
        Intent i = new Intent(context, PushService.class);
        if(intent != null)
        {
            mPushGlobals.setAppKey(intent.getStringExtra("app_key"));
            mPushGlobals.setAppMsgIntentServiceClassPath(intent.getStringExtra("app_service_path"));
        }
        i.setAction(ACTION_REGISTER);
        context.startService(i);
    }
    
    /**
     * Check GCM service is available or not, Prior to use GCM service.
     */
    private void checkGCMStatus()
    {
        if (PushGlobals.getInstance().isGCMChecked())
        {
            if (PushGlobals.getInstance().isGCMAvailabe())
            {
                registerWithGCM();
            }
            else
            {
                registerWithA2DM();
            }
        }
        else
        {
            if (RegisterManager.isGCMAvailable(this))
            {
                PushGlobals.getInstance().setGCMAvailabe(true);
                registerWithGCM();
            }
            else
            {
                registerWithA2DM();
            }
        }
    }

    /**
     * With GCM developer mGCMDeveloperId and GSF(google service framework), request reg_ID,
     * register or unregister from GCM with Intent("com.google.android.c2dm.intent.REGISTER") or
     * Intent("com.google.android.c2dm.intent.UNREGISTER").
     */
    private void registerWithGCM()
    {
        final String regId = RegisterManager.getRegistrationId(this);
        if (regId.equals(""))
        {
            // Automatically registers application on startup.
            RegisterManager.register(mAppContext, mGCMDeveloperId);
        }
        else
        {
            // Device is already registered on GCM, check CMS server.
            if (RegisterManager.isRegisteredOnCMSServer(this))
            {
                // Skips registration. already_registered
            }
            else
            {
                doRegisterTask();
            }
        }
    }

    /**
     * Try to register to CMS server, but not in the UI thread. It's also necessary to cancel the
     * thread onDestroy(), hence the use of AsyncTask instead of a raw thread.
     */
    private void doRegisterTask()
    {
        mRegisterCMSTask = new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground(Void... params)
            {
                boolean registered = RegisterRequest.registerCMSServer(PushService.this, "");
                // At this point all attempts to register with the app
                // server failed, so we need to unregister the device
                // from GCM - the app will try to register again when
                // it is restarted. Note that GCM will send an
                // unregistered callback upon completion, but
                // GCMIntentService.onUnregistered() will ignore it.
                if (!registered)
                {
                    RegisterManager.unregister(PushService.this);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result)
            {
                mRegisterCMSTask = null;
            }

        };
        mRegisterCMSTask.execute(null, null, null);
    }

    /**
     * The A2DM server will be instead of GCM server, when the GCM server is not available. The
     * process is simulator with GCM, and will keep alive and stable connection with A2DM.
     */
    private void registerWithA2DM()
    {
        PushA2DMService.actionStart(this);
    }
}

