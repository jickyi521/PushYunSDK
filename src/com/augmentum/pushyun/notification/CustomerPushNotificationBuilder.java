package com.augmentum.pushyun.notification;

import android.app.Notification;
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
    public Notification buildNotification(String title, String content)
    {
        if (StrUtils.isEmpty(content)) return null;
        
        Notification customerNotification = new Notification(this.mStatusBarIconDrawableId, title, System.currentTimeMillis());
        RemoteViews customerRemoteView = new RemoteViews(PushGlobals.getPackageName(), mCustomerLayout);
        customerRemoteView.setTextViewText(mLayoutTitleId, PushGlobals.getAppName());
        customerRemoteView.setTextViewText(mLayoutContentId, content);
        customerRemoteView.setImageViewResource(mLayoutIconId, mStatusBarIconDrawableId);
        customerNotification.contentView = customerRemoteView;
        customerNotification.flags |= Notification.FLAG_AUTO_CANCEL;

        return customerNotification;
    }

}
