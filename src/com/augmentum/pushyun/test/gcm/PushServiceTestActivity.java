package com.augmentum.pushyun.test.gcm;

import static com.augmentum.pushyun.PushGlobals.DISPLAY_MESSAGE_ACTION;
import static com.augmentum.pushyun.PushGlobals.EXTRA_MESSAGE;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;

import com.augmentum.pushyun.R;
import com.augmentum.pushyun.service.PushService;

public class PushServiceTestActivity extends Activity
{
    
    TextView mDisplay;
    AsyncTask<Void, Void, Void> mRegisterTask;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.gcm_main);
        mDisplay = (TextView)findViewById(R.id.display);
        
        registerReceiver(mHandleMessageReceiver, new IntentFilter(DISPLAY_MESSAGE_ACTION));
        PushService.register(this);
    }
    
    @Override
    protected void onDestroy()
    {
        if (mRegisterTask != null)
        {
            mRegisterTask.cancel(true);
        }
        unregisterReceiver(mHandleMessageReceiver);
        
        super.onDestroy();
    }
    
    private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String newMessage = intent.getExtras().getString(EXTRA_MESSAGE);
            mDisplay.append(newMessage + "\n");
        }
    };
}
