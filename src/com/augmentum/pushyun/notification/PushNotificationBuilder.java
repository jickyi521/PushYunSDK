package com.augmentum.pushyun.notification;

import android.app.Notification;


public interface PushNotificationBuilder
{
    public final static String NOTIFICATION_ID = "nid";
    
    /** 
     * @param notiifcationId Notification Id, use it when notify CMS to count
     * @param title Notification subject
     * @param content Notification Content
     * @return
     */
    Notification buildNotification(String notiificationId, String title, String content);
}
