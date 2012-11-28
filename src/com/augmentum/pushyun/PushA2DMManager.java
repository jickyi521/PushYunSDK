package com.augmentum.pushyun;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.util.Log;

import com.augmentum.pushyun.http.NetWorkInfo;
import com.augmentum.pushyun.service.PushService;

public class PushA2DMManager
{
    private static final String LOG_TAG = "PushA2DMManager";
    private static final long KEEP_ALIVE_INTERVAL = 900000L;// 15mins
    private static final String ACTION_KEEPALIVE = "com.augmentum.pushyun.service.KEEP_ALIVE";

    private static Context mContext;

    public static void initSoildA2DMConnection(Context context)
    {
        mContext = context;
        scheduleHeartbeatAlarm();
        context.registerReceiver(mConnectivityChanged, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    private static void scheduleHeartbeatAlarm()
    {
        Intent i = new Intent();
        i.setClass(mContext, PushService.class);
        i.setAction(ACTION_KEEPALIVE);
        PendingIntent pi = PendingIntent.getService(mContext, 0, i, 0);
        AlarmManager alarmMgr = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
        alarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + KEEP_ALIVE_INTERVAL,
                KEEP_ALIVE_INTERVAL, pi);
    }

    private static BroadcastReceiver mConnectivityChanged = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {

            boolean hasConnectivity = NetWorkInfo.isConnected(context);

            Log.v(LOG_TAG, "Connecting changed: connected=" + hasConnectivity);

            if (hasConnectivity)
            {

            }
            // reconnectIfNecessary();
        }
    };
}
