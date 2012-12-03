package com.augmentum.pushyun.test;

import static com.augmentum.pushyun.PushGlobals.DISPLAY_MESSAGE_ACTION;

import java.util.HashMap;

import android.content.Context;
import android.util.Log;

import com.augmentum.pushyun.PushGlobals;
import com.augmentum.pushyun.R;
import com.augmentum.pushyun.service.CoreMsgIntentService;

/**
 * IntentService responsible for handling GCM messages.
 */
public class MsgIntentService extends CoreMsgIntentService
{

    private static final String TAG = "GCMIntentService";

    @Override
    protected void onRegistered(Context context, String regId, boolean inGCM)
    {
        Log.i(TAG, "Device registered: regId = " + regId);
        PushGlobals.sendPushBroadcast(context, DISPLAY_MESSAGE_ACTION, getString(R.string.gcm_registered, inGCM ? "GCM" : "A2DM"));
    }
    
    @Override
    protected void onMessageDelivered(Context context, HashMap<String, String> msgMap)
    {
        Log.i(TAG, "Received message");
        String message = getString(R.string.gcm_message);
        PushGlobals.sendPushBroadcast(context, DISPLAY_MESSAGE_ACTION, message);
        // notifies user
        generateNotification(context, message);
        
    }
    
    @Override
    public void onError(Context context, String errorId)
    {
        Log.i(TAG, "Received error: " + errorId);
        PushGlobals.sendPushBroadcast(context, DISPLAY_MESSAGE_ACTION, getString(R.string.gcm_error, errorId));
    }

    /**
     * Issues a notification to inform the user that server has sent a message.
     */
    private static void generateNotification(Context context, String message)
    {
        // int icon = R.drawable.ic_stat_gcm;
        // long when = System.currentTimeMillis();
        // NotificationManager notificationManager = (NotificationManager)
        // context.getSystemService(Context.NOTIFICATION_SERVICE);
        // Notification notification = new Notification(icon, message, when);
        // String title = context.getString(R.string.app_name);
        // Intent notificationIntent = new Intent(context, GCMTestActivity.class);
        // // set intent so it does not start a new activity
        // notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
        // Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // PendingIntent intent =
        // PendingIntent.getActivity(context, 0, notificationIntent, 0);
        // notification.setLatestEventInfo(context, title, message, intent);
        // notification.flags |= Notification.FLAG_AUTO_CANCEL;
        // notificationManager.notify(0, notification);
    }

}
