package com.augmentum.pushyun.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.augmentum.pushyun.PushGlobals;
import com.augmentum.pushyun.common.Logger;
import com.augmentum.pushyun.service.CoreMsgIntentService;
import com.augmentum.pushyun.service.PushService;

/**
 * It's a core BroadcasrReceiver of pushyun, as a event action router, it will notify
 * {@link PushService} and {@link CoreMsgIntentService} according to receive actions.
 */
public class CoreBroadcastReceiver extends BroadcastReceiver
{
    @Override
    public final void onReceive(Context context, Intent intent)
    {
        String receiverAction = intent.getAction();
        Logger.verbose(Logger.RECEIVER_LOG_TAG, receiverAction);

        if (receiverAction.equals("android.intent.action.BOOT_COMPLETED") || receiverAction.equals("android.intent.action.USER_PRESENT"))
        {
            PushService.launchPushyunServiceIfRequired(context);
        }
        else
        {
            String className = PushGlobals.getPushConfigOptions().getAppIntentServicePath();
            if (className.equals(""))
            {
                className = getGCMIntentServiceClassName(context);
            }
            Logger.verbose(Logger.RECEIVER_LOG_TAG, "GCM IntentService class: " + className);

            CoreMsgIntentService.runIntentInService(context, intent, className);
            setResult(-1, null, null);
        }
    }

    protected String getGCMIntentServiceClassName(Context context)
    {
        return getDefaultIntentServiceClassName(context);
    }

    static final String getDefaultIntentServiceClassName(Context context)
    {
        String className = "com.augmentum.pushyun" + ".test.PushMsgIntentService";

        return className;
    }
}
