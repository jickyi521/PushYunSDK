package com.augmentum.pushyun.test;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.augmentum.pushyun.PushGlobals;
import com.augmentum.pushyun.R;

public class MainActivity extends Activity
{

    TextView mDisplay;
    Button mCounter;
    AsyncTask<Void, Void, Void> mRegisterTask;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mDisplay = (TextView)findViewById(R.id.display);
        mCounter = (Button)findViewById(R.id.counter);

        registerReceiver(mHandleMessageReceiver, new IntentFilter(PushGlobals.DISPLAY_MESSAGE_ACTION));
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

    /**
     * Handle receiver message.
     */
    private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String msg = intent.getExtras().getString(PushGlobals.EXTRA_MESSAGE);
            // mDisplay.setText(msg + "\n");
            mDisplay.append(msg + "\n");
            mCounter.setText("Message counter : "+String.valueOf(PushMsgIntentService.mCounter));
        }
    };
}
