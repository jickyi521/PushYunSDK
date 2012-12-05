package com.augmentum.pushyun;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.SystemClock;

import com.augmentum.pushyun.common.Logger;
import com.augmentum.pushyun.http.A2DMSolidConnThread;
import com.augmentum.pushyun.http.NetWorkInfo;
import com.augmentum.pushyun.service.PushService;

public class PushA2DMManager
{
    public static final String ACTION_DELIEVERED_MSG = "com.augmentum.pushyun.a2dm.intent.MESSAGE";
    
    private static final long KEEP_ALIVE_INTERVAL = 900000L;// 15mins
    private static final String ACTION_KEEPALIVE = "com.augmentum.pushyun.service.KEEP_ALIVE";

    private static Context mContext;
    private static A2DMSolidConnThread mA2DMSoildConnection;

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

            Logger.verbose(Logger.A2DM_CONNECTION_LOG_TAG, "Connecting changed: connected=" + hasConnectivity);

            if (hasConnectivity)
            {
                reconnectSolidA2DM();
            }
        }
    };
    
    private static void reconnectSolidA2DM()
    {
        if(mA2DMSoildConnection != null)
        {
            Logger.info(Logger.A2DM_CONNECTION_LOG_TAG, "Reconnecting to A2DM");
            long l = mA2DMSoildConnection.getRetryInterval();
            mA2DMSoildConnection.abort();
            mA2DMSoildConnection = new A2DMSolidConnThread(mContext);
            mA2DMSoildConnection.setRetryInterval(l);
          }
          else
          {
            Logger.info(Logger.A2DM_CONNECTION_LOG_TAG, "Starting new solid A2DM connection");
            mA2DMSoildConnection = new A2DMSolidConnThread(mContext);
          }
          mA2DMSoildConnection.start();
    }
}
