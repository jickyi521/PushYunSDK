package com.augmentum.pushyun.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONObject;

import android.content.Context;

import com.augmentum.pushyun.PushGlobals;
import com.augmentum.pushyun.common.Logger;
import com.augmentum.pushyun.common.PushException;
import com.augmentum.pushyun.http.response.BaseResponse;

public class A2DMSolidConnThread extends Thread
{
    private static final String LOG_TAG = "A2DMSolidConnThread";
    public static long MAX_KEEP_ALIVE_INTERVAL = 300000L;

    private static Context mContext;

    private final AtomicBoolean mRunning = new AtomicBoolean(false);
    private AtomicLong mLastSocketActivity;

    private long mRetryInterval = 10000L;

    private static Socket mSocket;
    private static InputStream mIn;
    private static OutputStream mOut;

    public A2DMSolidConnThread(Context context)
    {
        setName(LOG_TAG);
        mLastSocketActivity = new AtomicLong(0L);
        mContext = context;
    }

    @Override
    public void run()
    {
        super.run();

        try
        {
            connect();
        }
        catch (Exception e)
        {
            Logger.error("The A2DMSolid Connection Thread has died.");
            Logger.error(e);
        }
        finally
        {
            mRunning.set(false);
        }

    }

    private void connect()
    {
        Logger.verbose("A2DMSolidConnThread - run");
        mRunning.set(true);

        while (isRunning())
        {
            try
            {
                if (!preToLookUpA2DM()) { throw new PushException(PushException.A2DM_HTTP_ERROR, "A2DM server is not available"); }
                if (Thread.interrupted())
                {
                    Logger.debug("Thread interrupted during lookup.");
                    mRunning.set(false);
                    return;
                }
            }
            catch (PushException pushException)
            {
                Logger.error(pushException.getMessage());
                if (!sleepForRetryInterval(System.currentTimeMillis()))
                {
                    mRunning.set(false);
                    return;
                }
            }

            if (!isRunning())
            {
                Logger.debug("Connection sequence aborted. Ending prior to opening soild connection.");
                return;
            }

            long l = System.currentTimeMillis();
            try
            {
                mSocket = new Socket();
                mLastSocketActivity.set(System.currentTimeMillis());
                mSocket.setTcpNoDelay(false);
                mSocket.setSoTimeout((int)MAX_KEEP_ALIVE_INTERVAL);
                mSocket.connect(new InetSocketAddress(PushGlobals.A2DM_SERVER_HOST, PushGlobals.A2DM_SERVER_PORT), 60000);
                
                mOut = mSocket.getOutputStream();
                mOut.write("GET /api/mesage HTTP/1.1".getBytes());

                Logger.info("Connection established to " + mSocket.getInetAddress() + ":" + PushGlobals.A2DM_SERVER_PORT);

                while (isRunning())
                {
                    mIn = mSocket.getInputStream();
                    JSONObject jsonMessage = generateJSONData(mIn);
                    mLastSocketActivity.set(System.currentTimeMillis());
                    Thread.sleep(100L);
                    Logger.verbose(jsonMessage.toString());
                }

            }
            catch (Exception e)
            {
                Logger.debug("Connection thread interrupted.");
                mRunning.set(false);
                return;
            }
            finally
            {
                if (!isRunning())
                {
                    Logger.debug("Connection aborted, shutting down.");
                }
                else
                {
                    close(mSocket);
                    if (NetWorkInfo.isConnected(mContext))
                    {
                        if (sleepForRetryInterval(l)) continue;
                        mRunning.set(false);
                        return;
                    }
                    mRunning.set(false);
                }
            }
        }
    }

    public boolean isRunning()
    {
        return mRunning.get();
    }

    public long getRetryInterval()
    {
        return mRetryInterval;
    }
    
    public void setRetryInterval(long paramLong)
    {
      mRetryInterval = Math.min(paramLong, 600000L);
    }


    private boolean preToLookUpA2DM()
    {
        Get lookUp = new Get(PushGlobals.A2DM_SERVER_LOOK_UP_URL, null);
        BaseResponse lookUpResponse = lookUp.execute();
        return lookUpResponse.isStatusOk();
    }

    public void abort()
    {
        Logger.debug("Connection aborting.");
        mRunning.set(false);
        Logger.debug("Closing socket.");
        if (mSocket != null) close(mSocket);
        Logger.debug("Service stopped, socket closed successfully.");
    }

    /**
     * 
     * 
     * @param time Computing time
     * @return
     */
    private boolean sleepForRetryInterval(long time)
    {
        int i = 2;
        long l1 = mRetryInterval;
        long l2 = System.currentTimeMillis() - time;
        if (l2 < 180000L) l1 = Math.min(l1 * i, 600000L);
        else l1 = 10000L;
        Logger.debug("Rescheduling connection in " + l1 + "ms.");
        mRetryInterval = l1;
        try
        {
            Thread.sleep(l1);
            return true;
        }
        catch (InterruptedException localInterruptedException)
        {
        }
        return false;
    }

    private void close(Socket paramSocket)
    {
        if (paramSocket != null) try
        {
            if ((paramSocket.isConnected()) && (!paramSocket.isClosed())) paramSocket.close();
        }
        catch (IOException localIOException)
        {
            Logger.warn("Error closing socket.");
        }
    }

    public JSONObject generateJSONData(InputStream stream)
    {
        JSONObject jsonData = null;
        try
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(stream));
            StringBuffer buffer = new StringBuffer("");
            buffer = new StringBuffer("");
            String line = "";
            String NL = System.getProperty("line.separator");
            while ((line = in.readLine()) != null)
            {
                buffer.append(line + NL);
                if (!in.ready())
                {
                    break;
                }
            }
            jsonData = new JSONObject(buffer.toString());

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return jsonData;
    }
}
