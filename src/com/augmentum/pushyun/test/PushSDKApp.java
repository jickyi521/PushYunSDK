package com.augmentum.pushyun.test;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.augmentum.pushyun.R;
import com.augmentum.pushyun.notification.CustomerPushNotificationBuilder;
import com.augmentum.pushyun.notification.PushNotificationManager;
import com.augmentum.pushyun.service.PushService;

public class PushSDKApp extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();
        // the main process for this application is named for the package
        // if this is the default process, run some special code
        if (isProcess(getPackageName()))
        {
            
            // Recommend push Configuration properties way to launch
            PushService.launchPushyunService(this);
            // run default process operations here

            // Unregister the defined channel in the pushconfig.properties to CMS server.
            //PushService.unregisterChannelToCMS();
            
            setCustomerNotificationStyle();
        }
    }

    /**
     * It is possible to run the push service either in your application process or as a separate
     * stand-alone process. Recommend that applications run it in a separate process if they are
     * using the A2DM connection transport. The primary benefit to this approach is that it allows
     * the service to run with a relatively small memory footprint even if your application is
     * resource intensive. In a low memory situation the OS can kill a background application’s main
     * process without disrupting the service.
     * 
     * @param processName
     * @return
     */
    private boolean isProcess(String processName)
    {
        Context context = getApplicationContext();
        ActivityManager actMgr = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> appList = actMgr.getRunningAppProcesses();
        for (RunningAppProcessInfo info : appList)
        {
            if (info.pid == android.os.Process.myPid() && processName.equals(info.processName)) { return true; }
        }
        return false;
    }

    // Also support this way to launch
    public void startPushService()
    {
        Intent intent = new Intent();
        intent.putExtra("mGCMAppKey", "487820657755");// 179344231922
        intent.putExtra("mAPPIntentServicePath", "com.pushyun.test.PushMsgIntentService");
        intent.putExtra("mGCMEnabled", true);
        PushService.launchPushyunService(this, intent);
    }

    public void setCustomerNotificationStyle()
    {
        CustomerPushNotificationBuilder build = new CustomerPushNotificationBuilder(R.layout.customer_notification, R.id.subject,
                R.id.message, R.id.icon, R.drawable.top_logo, Uri.parse("file:///android_asset/notification.mp3"));

        PushNotificationManager.getInstance().setPushNotificationBuilder(build);

    }
}
