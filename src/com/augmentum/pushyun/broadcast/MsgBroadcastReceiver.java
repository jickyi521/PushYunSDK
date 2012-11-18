package com.augmentum.pushyun.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.augmentum.pushyun.manager.RegisterManager;
import com.augmentum.pushyun.service.MsgHandlerIntentService;

public class MsgBroadcastReceiver extends BroadcastReceiver
{
    private static final String LOG_TAG = "GCMBroadcastReceiver";
    private static boolean mReceiverSet = false;

    public final void onReceive(Context context, Intent intent)
    {
        Log.v(LOG_TAG, "onReceive: " + intent.getAction());

        if (!mReceiverSet)
        {
            mReceiverSet = true;
            String myClass = super.getClass().getName();
            if (!myClass.equals(MsgBroadcastReceiver.class.getName()))
            {
                RegisterManager.setRetryReceiverClassName(myClass);
            }
        }
        String className = getGCMIntentServiceClassName(context);
        
        Log.v("GCMBroadcastReceiver", "GCM IntentService class: " + className);

        MsgHandlerIntentService.runIntentInService(context, intent, className);
        setResult(-1, null, null);
    }

    protected String getGCMIntentServiceClassName(Context context)
    {
        return getDefaultIntentServiceClassName(context);
    }

    static final String getDefaultIntentServiceClassName(Context context)
    {
        String className = context.getPackageName() + ".test.MsgIntentService";

        return className;
    }
}
