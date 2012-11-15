package com.augmentum.pushyun.test.a2dm;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import com.augmentum.pushyun.R;
import com.augmentum.pushyun.service.PushA2DMService;

public class KeepAliveTestActivity extends Activity
{
    public static final String LOG_TAG = "TestKeepAlive";

    private final OnClickListener mClicked = new OnClickListener()
    {
        public void onClick(View v)
        {
            switch (v.getId())
            {
                case R.id.start:
                    PushA2DMService.actionStart(KeepAliveTestActivity.this);
                    break;
                case R.id.stop:
                    PushA2DMService.actionStop(KeepAliveTestActivity.this);
                    break;
                case R.id.ping:
                    PushA2DMService.actionPing(KeepAliveTestActivity.this);
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        setContentView(R.layout.keep_alvie_main);

        findViewById(R.id.start).setOnClickListener(mClicked);
        findViewById(R.id.stop).setOnClickListener(mClicked);
        findViewById(R.id.ping).setOnClickListener(mClicked);
    }
}