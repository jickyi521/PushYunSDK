package com.augmentum.pushyun.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.augmentum.pushyun.PushGlobals;
import com.augmentum.pushyun.util.StrUtils;

public class CustomerPushNotificationBuilder implements PushNotificationBuilder
{

    public int mCustomerLayout; // The layout resource to use with notification layout
    public int mLayoutTitleId; // The layout resource to display title
    public int mLayoutContentId; // The layout resource to display content
    public int mLayoutIconId; // The icon's id within customer layout
    public int mStatusBarIconDrawableId = PushGlobals.getAppInfo().icon; // The customer status icon
                                                                         // want to display
    public Uri mSoundUri;

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

        Notification customerNotification = new Notification(this.mStatusBarIconDrawableId, title, System.currentTimeMillis());
        RemoteViews customerRemoteView = new RemoteViews(PushGlobals.getPackageName(), mCustomerLayout);
        customerRemoteView.setTextViewText(mLayoutTitleId, PushGlobals.getAppName());
        customerRemoteView.setTextViewText(mLayoutContentId, content);
        customerRemoteView.setImageViewResource(mLayoutIconId, mStatusBarIconDrawableId);
        customerNotification.contentView = customerRemoteView;
        customerNotification.flags |= Notification.FLAG_AUTO_CANCEL;
        customerNotification.defaults = Notification.DEFAULT_SOUND;
        customerNotification.contentIntent = pengingIntent;

        return customerNotification;
    }

}
