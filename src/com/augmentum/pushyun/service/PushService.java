package com.augmentum.pushyun.service;

import static com.augmentum.pushyun.PushGlobals.DISPLAY_MESSAGE_ACTION;
import static com.augmentum.pushyun.PushGlobals.EXTRA_MESSAGE;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.augmentum.pushyun.PushGlobals;
import com.augmentum.pushyun.gcm.GCMRegistrar;
import com.augmentum.pushyun.request.RegisterRequest;

public class PushService extends Service
{
    private static final String LOG_TAG = "Pushervice";

    private static String mAppKey = "";
    private static String mToken = "";
    private static String mName = "";
    private static String mVersion = "";
    // GCM developer project id
    private static String mGCMDeveloperId = "";

    private static AsyncTask<Void, Void, Void> mRegisterCMSTask = null;

    // private static boolean mCheckedGCM = false;
    // private static boolean mGCMAvaiable = false;

    @Override
    public void onCreate()
    {
        super.onCreate();
        checkGCMStatus();
    }

    /**
     * Handle service request.
     */
    @Override
    public void onStart(Intent intent, int startId)
    {
        super.onStart(intent, startId);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    /**
     * Release android system resource。
     */
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (mRegisterCMSTask != null)
        {
            mRegisterCMSTask.cancel(true);
        }
        unregisterReceiver(mHandleMessageReceiver);
        GCMRegistrar.onDestroy(this);
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
            if (GCMRegistrar.isGCMAvailable(this))
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
        registerReceiver(mHandleMessageReceiver, new IntentFilter(DISPLAY_MESSAGE_ACTION));

        final String regId = GCMRegistrar.getRegistrationId(this);
        if (regId.equals(""))
        {
            // Automatically registers application on startup.
            GCMRegistrar.register(this, mGCMDeveloperId);
        }
        else
        {
            // Device is already registered on GCM, check CMS server.
            if (GCMRegistrar.isRegisteredOnCMSServer(this))
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
                    GCMRegistrar.unregister(PushService.this);
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

    /**
     * Handle receiver message.
     */
    private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String msg = intent.getExtras().getString(EXTRA_MESSAGE);
            Log.i(LOG_TAG, "************msg**************" + msg);
        }
    };

}

