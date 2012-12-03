package com.augmentum.pushyun.test.gcm;

import static com.augmentum.pushyun.PushGlobals.CMS_SERVER_REGISTER_URL;
import static com.augmentum.pushyun.PushGlobals.SENDER_ID;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.augmentum.pushyun.R;
import com.augmentum.pushyun.http.RegisterRequest;
import com.augmentum.pushyun.register.RegisterManager;

public class GCMTestActivity extends Activity
{
    TextView mDisplay;
    AsyncTask<Void, Void, Void> mRegisterTask;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        checkNotNull(CMS_SERVER_REGISTER_URL, "CMS_SERVER_REGISTER_URL");
        checkNotNull(SENDER_ID, "SENDER_ID");
        
        // Make sure the device has the proper dependencies.
        RegisterManager.checkDevice(this);
        
        // Make sure the manifest was properly set - comment out this line
        // while developing the app, then uncomment it when it's ready.
        RegisterManager.checkManifest(this);
        
        setContentView(R.layout.gcm_main);
        mDisplay = (TextView)findViewById(R.id.display);
        
        final String regId = RegisterManager.getRegistrationId();
        if (regId.equals(""))
        {
            // Automatically registers application on startup.
            RegisterManager.registerInGCM(this, SENDER_ID);
        }
        else
        {
            // Device is already registered on GCM, check server.
            if (RegisterManager.isRegisteredOnCMSServer())
            {
                // Skips registration.
                mDisplay.append(getString(R.string.already_registered) + "\n");
            }
            else
            {
                // Try to register again, but not in the UI thread.
                // It's also necessary to cancel the thread onDestroy(),
                // hence the use of AsyncTask instead of a raw thread.
                
                doRegisterTask();
            }
        }
    }
    
    private void doRegisterTask()
    {
        final Context context = this;
        mRegisterTask = new AsyncTask<Void, Void, Void>()
        {

            @Override
            protected Void doInBackground(Void... params)
            {
                boolean registered = RegisterRequest.registerCMSServer(context, "");
                // At this point all attempts to register with the app
                // server failed, so we need to unregister the device
                // from GCM - the app will try to register again when
                // it is restarted. Note that GCM will send an
                // unregistered callback upon completion, but
                // GCMIntentService.onUnregistered() will ignore it.
                if (!registered)
                {
                    RegisterManager.unregister(context);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result)
            {
                mRegisterTask = null;
            }

        };
        mRegisterTask.execute(null, null, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        /*
         * Typically, an application registers automatically, so options below are disabled.
         * Uncomment them if you want to manually register or unregister the device (you will also
         * need to uncomment the equivalent options on options_menu.xml).
         */
        /*
         * case R.id.options_register: GCMRegistrar.register(this, SENDER_ID); return true; case
         * R.id.options_unregister: GCMRegistrar.unregister(this); return true;
         */
            case 0x7f070004://R.id.options_clear:
                mDisplay.setText(null);
                return true;
            case 0x7f070005://R.id.options_exit:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy()
    {
        if (mRegisterTask != null)
        {
            mRegisterTask.cancel(true);
        }
        RegisterManager.onDestroy(this);
        super.onDestroy();
    }

    private void checkNotNull(Object reference, String name)
    {
        if (reference == null) { throw new NullPointerException(getString(R.string.error_config, name)); }
    }

}