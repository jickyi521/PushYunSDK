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

    private static Context mContext;
    private static A2DMSolidConnThread mA2DMSoildConnection;

    private static String mIpAddress;

    public static void initSoildA2DMConnection(Context context)
    {
        mContext = context;
        scheduleHeartbeatAlarm();
        clearA2DMConnection();
        context.registerReceiver(mConnectivityChanged, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public static void resetStuckConnection()
    {
        Logger.verbose(Logger.A2DM_CONNECTION_LOG_TAG, "PushA2DMManager - resetStuckConnection()");
        if ((mA2DMSoildConnection == null) || (!NetWorkInfo.isConnected())) return;
        if (!mA2DMSoildConnection.isRunning())
        {
            Logger.verbose(Logger.A2DM_CONNECTION_LOG_TAG, "PushA2DMManager - reconnecting a closed connection.");
            reconnectSolidA2DM();
        }
        else
        {
            Logger.verbose(Logger.A2DM_CONNECTION_LOG_TAG,
                    "PushA2DMManager - checking the state of the Helium connection to see if it needs a reset.");
            mA2DMSoildConnection.resetStaleConnection();
        }
    }
    
    public static void teardown()
    {
      Logger.verbose(Logger.A2DM_CONNECTION_LOG_TAG, "Embedded Push teardown!");
      if (mConnectivityChanged != null)
      {
        mContext.unregisterReceiver(mConnectivityChanged);
        mConnectivityChanged = null;
      }
      clearA2DMConnection();
    }

    private static void scheduleHeartbeatAlarm()
    {
        Intent i = new Intent();
        i.setClass(mContext, PushService.class);
        i.setAction(PushService.ACTION_HEART_BEAT);
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

            boolean hasConnectivity = NetWorkInfo.isConnected();

            Logger.verbose(Logger.A2DM_CONNECTION_LOG_TAG, "Connecting changed: connected=" + hasConnectivity);

            if (hasConnectivity)
            {
                if (intent != null)
                {
                    if (intent.getBooleanExtra("isFailover", false)) Logger.verbose(Logger.A2DM_CONNECTION_LOG_TAG, "Network failover.");
                }
                if ((mA2DMSoildConnection == null) || (!mA2DMSoildConnection.isRunning()) || ipChanged()) reconnectSolidA2DM();
            }
            else
            {
                Logger.info(Logger.A2DM_CONNECTION_LOG_TAG, "Connectivity lost, shutting down helium connection");
                clearA2DMConnection();
            }
        }
    };

    private static void reconnectSolidA2DM()
    {
        if (mA2DMSoildConnection != null)
        {
            Logger.info(Logger.A2DM_CONNECTION_LOG_TAG, "Reconnecting to A2DM");
            long l = mA2DMSoildConnection.getRetryInterval();
            mA2DMSoildConnection.abort();
            mA2DMSoildConnection = new A2DMSolidConnThread();
            mA2DMSoildConnection.setRetryInterval(l);
        }
        else
        {
            Logger.info(Logger.A2DM_CONNECTION_LOG_TAG, "Starting new solid A2DM connection");
            mA2DMSoildConnection = new A2DMSolidConnThread();
        }
        setIPAddress(NetWorkInfo.getActiveIPAddress());
        mA2DMSoildConnection.start();
    }

    private static void clearA2DMConnection()
    {
        setIPAddress(null);
        if (mA2DMSoildConnection != null)
        {
            mA2DMSoildConnection.abort();
            mA2DMSoildConnection = null;
        }
    }

    private static void setIPAddress(String ip)
    {
        mIpAddress = ip;
    }

    private static boolean ipChanged()
    {
        String str = NetWorkInfo.getActiveIPAddress();
        Logger.verbose(Logger.A2DM_CONNECTION_LOG_TAG, "Current IP: " + str + ". Previous IP: " + mIpAddress);
        boolean bool;
        if ((mIpAddress == null) && (str != null)) bool = true;
        else if ((mIpAddress != null) && (str != null) && (!mIpAddress.equals(str))) bool = true;
        else bool = false;
        Logger.verbose(Logger.A2DM_CONNECTION_LOG_TAG, "IP Changed: " + bool);
        return bool;
    }
}
