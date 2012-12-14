package com.augmentum.pushyun.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;

import com.augmentum.pushyun.PushGlobals;
import com.augmentum.pushyun.util.StrUtils;

public class BasicPushNotificationBuilder implements PushNotificationBuilder
{

    public int mIconDrawableId = PushGlobals.getAppInfo().icon;

    @Override
    public Notification buildNotification(String notiificationId, String title, String content)
    {
        if (StrUtils.isEmpty(content)) return null;

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.setComponent(new ComponentName(PushGlobals.getPackageName(), PushGlobals.getLaunchActivityPathName()));
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.putExtra(PushNotificationBuilder.NOTIFICATION_ID, notiificationId);
        PendingIntent pengingIntent = PendingIntent.getActivity(PushGlobals.getAppContext(), 0, intent, 0);

        Notification basicNotification = new Notification(this.mIconDrawableId, title, System.currentTimeMillis());
        basicNotification.setLatestEventInfo(PushGlobals.getAppContext(), title, content, pengingIntent);
        basicNotification.flags |= Notification.FLAG_AUTO_CANCEL;
        basicNotification.defaults = Notification.DEFAULT_SOUND;
        basicNotification.contentIntent = pengingIntent;

        return basicNotification;
    }

}
