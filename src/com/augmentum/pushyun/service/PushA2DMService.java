package com.augmentum.pushyun.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;

import com.augmentum.pushyun.PushGlobals;
import com.augmentum.pushyun.common.ConnectionLog;
import com.augmentum.pushyun.test.a2dm.KeepAliveTestActivity;

public class PushA2DMService extends Service
{

    public static final String LOG_TAG = "PushA2DMService";
    public static final String ACTION_REGISTER = "com.augmentum.pushyun.a2dm.intent.REGISTRATION";
    public static final String ACTION_DELIEVERED_MSG = "com.augmentum.pushyun.a2dm.intent.MESSAGE";

    private static final String ACTION_START = "com.augmentum.pushyun.service.START";
    private static final String ACTION_STOP = "com.augmentum.pushyun.service.STOP";
    private static final String ACTION_KEEPALIVE = "com.augmentum.pushyun.service.KEEP_ALIVE";
    private static final String ACTION_RECONNECT = "com.augmentum.pushyun.service.RECONNECT";
    private static final String PREF_STARTED = "isStarted";
    private static final String A2DM_REGISTER_URL = PushGlobals.A2DM_SERVER_REGISTER_URL;

    private static final long KEEP_ALIVE_INTERVAL = 1000 * 60 * 28;
    private static final long INITIAL_RETRY_INTERVAL = 1000 * 10;
    private static final long MAXIMUM_RETRY_INTERVAL = 1000 * 60 * 30;

    private static final int NOTIF_CONNECTED = 0;

    private ConnectionLog mConnectionLog;

    private ConnectivityManager mConnMan;
    private NotificationManager mNotifMan;

    private boolean mStarted;
    private A2DMConnectionThread mConnection;
    private SharedPreferences mPrefs;

    public static void actionStart(Context ctx)
    {
        Intent i = new Intent(ctx, PushA2DMService.class);
        i.setAction(ACTION_START);
        ctx.startService(i);
    }

    public static void actionStop(Context ctx)
    {
        Intent i = new Intent(ctx, PushA2DMService.class);
        i.setAction(ACTION_STOP);
        ctx.startService(i);
    }

    public static void actionPing(Context ctx)
    {
        Intent i = new Intent(ctx, PushA2DMService.class);
        i.setAction(ACTION_KEEPALIVE);
        ctx.startService(i);
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        try
        {
            mConnectionLog = new ConnectionLog();
            Log.i(LOG_TAG, "Opened log at " + mConnectionLog.getPath());
        }
        catch (IOException e)
        {
        }

        mPrefs = getSharedPreferences(LOG_TAG, MODE_PRIVATE);

        mConnMan = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);

        mNotifMan = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        /*
         * If our process was reaped by the system for any reason we need to restore our state with
         * merely a call to onCreate. We record the last "started" value and restore it here if
         * necessary.
         */
        handleCrashedService();
    }

    private void handleCrashedService()
    {
        if (wasStarted() == true)
        {
            /*
             * We probably didn't get a chance to clean up gracefully, so do it now.
             */
            hideNotification();
            stopKeepAlives();

            /* Formally start and attempt connection. */
            start();
        }
    }

    @Override
    public void onDestroy()
    {
        logMsg("Service destroyed (started=" + mStarted + ")");

        if (mStarted == true) stop();

        try
        {
            if (mConnectionLog != null) mConnectionLog.close();
        }
        catch (IOException e)
        {
        }
    }

    private void logMsg(String message)
    {
        Log.v(LOG_TAG, message);

        if (mConnectionLog != null)
        {
            try
            {
                mConnectionLog.println(message);
            }
            catch (IOException e)
            {
            }
        }
    }

    private boolean wasStarted()
    {
        return mPrefs.getBoolean(PREF_STARTED, false);
    }

    private void setStarted(boolean started)
    {
        mPrefs.edit().putBoolean(PREF_STARTED, started).commit();
        mStarted = started;
    }

    @Override
    public void onStart(Intent intent, int startId)
    {
        logMsg("Service started with intent=" + intent);

        super.onStart(intent, startId);

        testStartCommand(intent);

        if (intent.getAction().equals(ACTION_STOP) == true)
        {
            stop();
            stopSelf();
        }
        else if (intent.getAction().equals(ACTION_START) == true)
        {
            start();

        }
        else if (intent.getAction().equals(ACTION_KEEPALIVE) == true) keepAlive();
        else if (intent.getAction().equals(ACTION_RECONNECT) == true) reconnectIfNecessary();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        throw new IllegalArgumentException("You cannot bind directly to the PushA2DMService.");
    }

    private synchronized void start()
    {
        if (mStarted == true)
        {
            Log.w(LOG_TAG, "Attempt to start connection that is already active");
            return;
        }

        setStarted(true);

        registerReceiver(mConnectivityChanged, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        logMsg("Connecting...");

        mConnection = new A2DMConnectionThread(A2DM_REGISTER_URL);
        mConnection.start();
    }

    private synchronized void stop()
    {
        if (mStarted == false)
        {
            Log.w(LOG_TAG, "Attempt to stop connection not active.");
            return;
        }

        setStarted(false);

        unregisterReceiver(mConnectivityChanged);
        cancelReconnect();

        if (mConnection != null)
        {
            mConnection.abort();
            mConnection = null;
        }
    }

    private synchronized void keepAlive()
    {
        try
        {
            if (mStarted == true && mConnection != null) mConnection.sendKeepAlive();
        }
        catch (IOException e)
        {
        }
    }

    private void startKeepAlives()
    {
        Intent i = new Intent();
        i.setClass(this, PushA2DMService.class);
        i.setAction(ACTION_KEEPALIVE);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        AlarmManager alarmMgr = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + KEEP_ALIVE_INTERVAL, KEEP_ALIVE_INTERVAL, pi);
    }

    private void stopKeepAlives()
    {
        Intent i = new Intent();
        i.setClass(this, PushA2DMService.class);
        i.setAction(ACTION_KEEPALIVE);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        AlarmManager alarmMgr = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarmMgr.cancel(pi);
    }

    public void scheduleReconnect(long startTime)
    {
        long interval = mPrefs.getLong("retryInterval", INITIAL_RETRY_INTERVAL);

        long now = System.currentTimeMillis();
        long elapsed = now - startTime;

        if (elapsed < interval) interval = Math.min(interval * 4, MAXIMUM_RETRY_INTERVAL);
        else interval = INITIAL_RETRY_INTERVAL;

        logMsg("Rescheduling connection in " + interval + "ms.");

        mPrefs.edit().putLong("retryInterval", interval).commit();

        Intent i = new Intent();
        i.setClass(this, PushA2DMService.class);
        i.setAction(ACTION_RECONNECT);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        AlarmManager alarmMgr = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, now + interval, pi);
    }

    public void cancelReconnect()
    {
        Intent i = new Intent();
        i.setClass(this, PushA2DMService.class);
        i.setAction(ACTION_RECONNECT);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        AlarmManager alarmMgr = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarmMgr.cancel(pi);
    }

    private synchronized void reconnectIfNecessary()
    {
        if (mStarted == true && mConnection == null)
        {
            logMsg("Reconnecting...");

            mConnection = new A2DMConnectionThread(A2DM_REGISTER_URL);
            mConnection.start();
        }
    }

    private BroadcastReceiver mConnectivityChanged = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            NetworkInfo info = mConnMan.getActiveNetworkInfo();
            // NetworkInfo info =
            // (NetworkInfo)intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

            boolean hasConnectivity = (info != null && info.isConnected()) ? true : false;

            logMsg("Connecting changed: connected=" + hasConnectivity);

            if (hasConnectivity) reconnectIfNecessary();
        }
    };

    //TODO Define notification status bar style, logo and text
    private void showNotification()
    {
        Notification n = new Notification();

        n.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

        //n.icon = R.drawable.logo;
        n.when = System.currentTimeMillis();

        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, KeepAliveTestActivity.class), 0);

        n.setLatestEventInfo(this, "KeepAlive connected", "Connected to " + A2DM_REGISTER_URL, pi);

        mNotifMan.notify(NOTIF_CONNECTED, n);
    }

    private void hideNotification()
    {
        mNotifMan.cancel(NOTIF_CONNECTED);
    }

    private class A2DMConnectionThread extends Thread
    {

        private final HttpClient mHttpClient;

        private final String mUrl;

        public A2DMConnectionThread(String url)
        {
            mUrl = url;
            mHttpClient = new DefaultHttpClient();
        }

        private boolean isNetworkAvailable()
        {
            NetworkInfo info = mConnMan.getActiveNetworkInfo();
            if (info == null) return false;

            return info.isConnected();
        }

        public void run()
        {
            executeHttpGet();
        }

        // TODO refine it with register, message api
        public void executeHttpGet()
        {

            long startTime = System.currentTimeMillis();
            BufferedReader in = null;
            try
            {
                startKeepAlives();
                showNotification();

                HttpClient client = mHttpClient;
                HttpGet request = new HttpGet();
                // need to find a way to set socket timeout
                client.getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 10000);
                client.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);
                // Test
                request.setURI(new URI(mUrl + "?udid=test&app=app1"));

                logMsg("Connection established to " + request.getURI().toString());

                // Ensure the stream is appropriately managed and released back into the pool of
                // idle connections.
//                HttpResponse response = client.execute(request, new ResponseHandler<HttpResponse>()
//                {
//
//                    @Override
//                    public HttpResponse handleResponse(HttpResponse response) throws ClientProtocolException, IOException
//                    {
//                        return response;
//                    }
//                });
                 HttpResponse response = client.execute(request);

                in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

                StringBuffer sb = new StringBuffer("");
                String line = "";
                String NL = System.getProperty("line.separator");
                while ((line = in.readLine()) != null)
                {
                    sb.append(line + NL);
                    if (!in.ready())
                    {
                        break;
                    }
                }

                JSONObject jSONObject = new JSONObject(sb.toString());

                if (response.getStatusLine().getStatusCode() == 200)
                {
                    String token = jSONObject.getString("token");
                    sendRegisterBroadcast(token);
                }

                Log.i(LOG_TAG, jSONObject.toString());
            }
            catch (Exception e)
            {
                Log.v(LOG_TAG, "Unexpected Exception: " + e.toString());
            }
            finally
            {
                stopKeepAlives();
                hideNotification();

                if (in != null)
                {
                    try
                    {
                        in.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }

                    synchronized (PushA2DMService.this)
                    {
                        mConnection = null;
                    }

                    /*
                     * If our local interface is still up then the connection failure must have been
                     * something intermittent. Try our connection again later (the wait grows with
                     * each successive failure). Otherwise we will try to reconnect when the local
                     * interface comes back.
                     */
                    if (isNetworkAvailable() == true) scheduleReconnect(startTime);
                }
            }
        }

        // @TODO Ping the server
        public void sendKeepAlive() throws IOException
        {
            // HttpClient client = mHttpClient;
            // Date d = new Date();
            logMsg("Keep-alive sent.");
        }

        public void abort()
        {
            logMsg("Connection aborting.");

            try
            {
                mHttpClient.getConnectionManager().shutdown();
            }
            catch (Exception e)
            {
            }

            // Blocks the current Thread until the receiver finishes its execution and dies.
            while (true)
            {
                try
                {
                    join();
                    break;
                }
                catch (InterruptedException e)
                {
                }
            }
        }
    }

    private void sendRegisterBroadcast(String token)
    {
        Intent intent = new Intent(ACTION_REGISTER);
        intent.putExtra("registration_id", token);
        sendBroadcast(intent);
    }

    private void testStartCommand(Intent intent)
    {
        // String token = intent.getStringExtra("app");
        // Log.v(LOG_TAG,
        // "*******action******" + intent.getAction() + "******token*****" + token +
        // "********package******" + intent.getPackage());
        //
        // Intent appIntent = new Intent("com.google.android.a2dm.intent.RECEIVER");
        // appIntent.setPackage("com.tokudu.demo");
        // appIntent.putExtra("msg", "receive msg from push");
        // sendBroadcast(appIntent);
    }
}