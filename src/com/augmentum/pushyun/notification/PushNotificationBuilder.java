package com.augmentum.pushyun.notification;

import android.app.Notification;


public interface PushNotificationBuilder
{
    /**
     * @param title Notification subject
     * @param content Notification Content
     * @return
     */
    Notification buildNotification(String title, String content);
}
