package com.augmentum.pushyun.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.augmentum.pushyun.PushGlobals;
import com.augmentum.pushyun.PushPreferences;
import com.augmentum.pushyun.R;
import com.augmentum.pushyun.common.Logger;

/**
 * @TODO Notification manager, including system and customer's logo, layout styles
 */
public class PushNotificationManager
{
    private static final String NOTIFICATION_ID_KEY = "com.augmentum.pushyun.notification.ID";

    private static PushNotificationManager mPushNotificationManager = new PushNotificationManager();

    private PushNotificationBuilder mPushNotificationBuilder = new BasicPushNotificationBuilder();

    private int mStartId = 1000;
    private int mRange = 40;

    private PushNotificationManager()
    {
        // Empty
    }

    public static PushNotificationManager getInstance()
    {
        if (mPushNotificationManager == null)
        {
            mPushNotificationManager = new PushNotificationManager();
        }
        return mPushNotificationManager;
    }

    public PushNotificationBuilder getPushNotificationBuilder()
    {
        return mPushNotificationBuilder;
    }

    public void setPushNotificationBuilder(PushNotificationBuilder pushNotificationBuilder)
    {
        mPushNotificationBuilder = pushNotificationBuilder;
    }

    public void deliverPushNotification(String title, String content)
    {
        buildAndDisplayNotification(title, content);
    }

    private int generateNotificationId()
    {
        int i = PushPreferences.getInstance().getIntValue(NOTIFICATION_ID_KEY, mStartId);
        i++;
        int j = i;
        if (j < mStartId + mRange)
        {
            Logger.verbose(Logger.NOTIFICATION_LOG_TAG, "Incrementing notification id count");
            PushPreferences.getInstance().setIntValue(NOTIFICATION_ID_KEY, j);
        }
        else
        {
            Logger.verbose(Logger.NOTIFICATION_LOG_TAG, "Resetting notification id count");
            PushPreferences.getInstance().setIntValue(NOTIFICATION_ID_KEY, mStartId);
        }
        Logger.verbose(Logger.NOTIFICATION_LOG_TAG, "Notification id: " + i);
        return i;
    }

    // Refine : multi notification layout style
    private void buildAndDisplayNotification(String title, String content)
    {
        if (mPushNotificationBuilder != null)
        {
            Notification notification = mPushNotificationBuilder.buildNotification(title, content);
            if (notification != null)
            {
                notification.flags |= Notification.FLAG_AUTO_CANCEL;
                NotificationManager notificationManager = (NotificationManager)PushGlobals.getAppContext().getSystemService(
                        Context.NOTIFICATION_SERVICE);
                notificationManager.notify(generateNotificationId(), notification);

                Logger.verbose(Logger.NOTIFICATION_LOG_TAG, "delieve to app successfully, message : ***** " + content + "****");
            }
        }
    }

    /**
     * Issues a notification to inform the user that server has sent a message.
     */
    @Deprecated
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
