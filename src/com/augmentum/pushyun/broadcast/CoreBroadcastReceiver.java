package com.augmentum.pushyun.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.augmentum.pushyun.PushGlobals;
import com.augmentum.pushyun.register.RegisterManager;
import com.augmentum.pushyun.service.CoreMsgIntentService;

public class CoreBroadcastReceiver extends BroadcastReceiver
{
    private static final String LOG_TAG = "MsgBroadcastReceiver";
    private static boolean mReceiverSet = false;

    @Override
    public final void onReceive(Context context, Intent intent)
    {
        Log.v(LOG_TAG, "onReceive: " + intent.getAction());
        
        if (!mReceiverSet)
        {
            mReceiverSet = true;
            String myClass = super.getClass().getName();
            if (!myClass.equals(CoreBroadcastReceiver.class.getName()))
            {
                RegisterManager.setRetryReceiverClassName(myClass);
            }
        }
        String className = PushGlobals.getPushConfigOptions().getAppIntentServicePath();
        if (className.equals(""))
        {
            className = getGCMIntentServiceClassName(context);
        }

        Log.v(LOG_TAG, "GCM IntentService class: " + className);

        CoreMsgIntentService.runIntentInService(context, intent, className);
        setResult(-1, null, null);
    }

    protected String getGCMIntentServiceClassName(Context context)
    {
        return getDefaultIntentServiceClassName(context);
    }

    static final String getDefaultIntentServiceClassName(Context context)
    {
        String className = "com.augmentum.pushyun" + ".test.MsgIntentService";

        return className;
    }
}
