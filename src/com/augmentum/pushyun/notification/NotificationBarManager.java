package com.augmentum.pushyun.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.augmentum.pushyun.R;

/**
 * @TODO Notification manager, including system and customer's logo, layout styles
 */
public class NotificationBarManager
{
    /**
     * Issues a notification to inform the user that server has sent a message.
     */
    public static void showNotification(Context context, String message)
    {
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification();
        String title = context.getString(R.string.app_name);
        Intent notificationIntent = new Intent();
        // set intent so it does not start a new activity
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        notification.setLatestEventInfo(context, title, message, intent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(0, notification);
    }
}
