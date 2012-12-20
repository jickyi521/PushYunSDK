package com.augmentum.pushyun.http;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import com.augmentum.pushyun.PushManager;
import com.augmentum.pushyun.common.Logger;
import com.augmentum.pushyun.common.PushException;
import com.augmentum.pushyun.common.PushyunConfigOptions;
import com.augmentum.pushyun.http.response.BaseResponse;
import com.augmentum.pushyun.register.RegisterManager;
import com.augmentum.pushyun.task.HttpCallBack;
import com.augmentum.pushyun.util.SignUtils;

public class A2DMSolidConnThread extends Thread
{
    public static long MAX_KEEP_ALIVE_INTERVAL = 300000L;

    private final AtomicBoolean mRunning = new AtomicBoolean(false);
    private AtomicLong mLastSocketActivity;

    private long mRetryInterval = 10000L;

    private static Socket mSocket;
    private static InputStream mIn;
    private static OutputStream mOut;
    private static BufferedWriter mWr;

    public A2DMSolidConnThread()
    {
        setName("A2DMSolidConnThread");
        mLastSocketActivity = new AtomicLong(0L);
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
            Logger.error(Logger.A2DM_CONNECTION_LOG_TAG, "The A2DMSolid Connection Thread has died.");
            Logger.error(Logger.A2DM_CONNECTION_LOG_TAG, e);
        }
        finally
        {
            mRunning.set(false);
        }

    }

    private void connect()
    {
        Logger.verbose(Logger.A2DM_CONNECTION_LOG_TAG, "A2DMSolidConnThread - run");
        mRunning.set(true);

        while (isRunning())
        {
            try
            {
                if (!preToLookUpA2DM()) { throw new PushException(PushException.A2DM_HTTP_ERROR, "A2DM server is not available"); }
                if (Thread.interrupted())
                {
                    Logger.debug(Logger.A2DM_CONNECTION_LOG_TAG, "Thread interrupted during lookup.");
                    mRunning.set(false);
                    return;
                }
            }
            catch (PushException pushException)
            {
                Logger.error(Logger.A2DM_CONNECTION_LOG_TAG, pushException.getMessage());
                if (!sleepForRetryInterval(System.currentTimeMillis()))
                {
                    mRunning.set(false);
                    return;
                }
            }

            if (!isRunning())
            {
                Logger.debug(Logger.A2DM_CONNECTION_LOG_TAG, "Connection sequence aborted. Ending prior to opening soild connection.");
                return;
            }

            long l = System.currentTimeMillis();
            try
            {
                //Test
                //retrieveConnector();
                
                mSocket = new Socket();
                mLastSocketActivity.set(System.currentTimeMillis());
                mSocket.setTcpNoDelay(false);
                mSocket.setSoTimeout((int)MAX_KEEP_ALIVE_INTERVAL);
                // TODO just for test mSocket.connect(new InetSocketAddress(PushGlobals.A2DM_SERVER_HOST, PushGlobals.A2DM_SERVER_PORT), 60000);
                mSocket.connect(new InetSocketAddress("192.168.196.135", 3006), 60000);

                mOut = mSocket.getOutputStream();

                TreeMap<String, String> apiParamsMap = new TreeMap<String, String>();

                String appKey = PushyunConfigOptions.getInstance().getA2DMAppKey();
                String token = RegisterManager.getRegistrationId();
                String apiKey = PushyunConfigOptions.getInstance().getAPIKey();
                String sign = SignUtils.generateSignature(apiParamsMap, PushyunConfigOptions.getInstance().getAPISecret());
                String data = "";

                apiParamsMap.put("appKey", appKey);
                apiParamsMap.put("token", token);
                apiParamsMap.put("apiKey", apiKey);

                data = "appKey=" + appKey + "&token=" + token + "&apiKey=" + apiKey + "&sign=" + sign;

                mWr = new BufferedWriter(new OutputStreamWriter(mOut, "UTF8"));

                mWr.write("POST /client/mesage HTTP/1.1\r\n");
                mWr.write("Connection: close\r\n");
                mWr.write("Content-Length: " + data.length() + "\r\n");
                mWr.write("Content-Type: application/x-www-form-urlencoded\r\n");
                mWr.write("\r\n");
                mWr.write(data);
                mWr.flush();

                Logger.info(Logger.A2DM_CONNECTION_LOG_TAG, "Connection established to " + mSocket.getInetAddress() + ":"
                        + HttpParams.A2DM_SERVER_PORT);

                while (isRunning())
                {
                    mIn = mSocket.getInputStream();
                    JSONObject jsonMessage = generateJSONData(mIn);
                    mLastSocketActivity.set(System.currentTimeMillis());
                    Thread.sleep(100L);
                    Logger.verbose(Logger.A2DM_CONNECTION_LOG_TAG, jsonMessage.toString());
                }

            }
            catch (Exception e)
            {
                Logger.debug(Logger.A2DM_CONNECTION_LOG_TAG, "Connection thread interrupted.");
                mRunning.set(false);
                return;
            }
            finally
            {
                if (!isRunning())
                {
                    Logger.debug(Logger.A2DM_CONNECTION_LOG_TAG, "Connection aborted, shutting down.");
                }
                else
                {
                    close(mSocket);
                    if (NetWorkInfo.isConnected())
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

    /**
     * @TODO refine it get the connector server address
     * @return
     */
    private boolean preToLookUpA2DM()
    {
        Get lookUp = new Get(HttpParams.A2DM_SERVER_LOOK_UP_URL, null);
        BaseResponse lookUpResponse = lookUp.execute();
        return lookUpResponse.isStatusOk();
    }
    
    //@TODO retrieve connector url before establishing with a connection 
    public String retrieveConnector()
    {
        String connectorUrl = "";
        
        TreeMap<String, String> apiParamsMap = new TreeMap<String, String>();

        apiParamsMap.put(HttpParams.appKey, HttpParams.appKeyValue);
        apiParamsMap.put(HttpParams.apiKey, HttpParams.apiKeyValue);
        apiParamsMap.put(HttpParams.sign, SignUtils.generateSignature(apiParamsMap, PushyunConfigOptions.getInstance().getAPISecret()));

        List<BasicNameValuePair> nameValuePairs = new ArrayList<BasicNameValuePair>();

        Iterator<?> iter = apiParamsMap.entrySet().iterator();
        while (iter.hasNext())
        {
            @SuppressWarnings("rawtypes")
            Map.Entry entry = (Map.Entry)iter.next();
            nameValuePairs.add(new BasicNameValuePair(entry.getKey().toString(), entry.getValue().toString()));
        }

        Post post = new Post(HttpParams.A2DM_SERVER_CONNECTOR_URL, nameValuePairs);

        PushManager.executeHttpRequest(post, HttpParams.POST_METHOD, new HttpCallBack()
        {
            @Override
            public void done(BaseResponse respone, PushException e)
            {
                if (e == null && respone != null)
                {
                    if (respone.isStatusOk())
                    {
                        //get the connctor url
                    }
                }
            }
        });
        
        return connectorUrl;
    }

    public void abort()
    {
        Logger.debug(Logger.A2DM_CONNECTION_LOG_TAG, "Connection aborting.");
        mRunning.set(false);
        Logger.debug(Logger.A2DM_CONNECTION_LOG_TAG, "Closing socket.");
        if (mSocket != null) close(mSocket);
        Logger.debug(Logger.A2DM_CONNECTION_LOG_TAG, "Service stopped, socket closed successfully.");
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
        Logger.debug(Logger.A2DM_CONNECTION_LOG_TAG, "Rescheduling connection in " + l1 + "ms.");
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

    public void resetStaleConnection()
    {
        if (isSocketStale())
        {
            Logger.verbose(Logger.A2DM_CONNECTION_LOG_TAG, "Helium connection is stale. Closing socket to force retry.");
            close(mSocket);
        }
    }

    private boolean isSocketStale()
    {
        Logger.verbose(Logger.A2DM_CONNECTION_LOG_TAG, "Checking socket status:");
        if (!isRunning())
        {
            Logger.verbose(Logger.A2DM_CONNECTION_LOG_TAG, "The connection is not running.");
            return false;
        }
        if (mSocket == null)
        {
            Logger.verbose(Logger.A2DM_CONNECTION_LOG_TAG, "Socket is null.");
            return false;
        }
        if (!mSocket.isConnected())
        {
            Logger.verbose(Logger.A2DM_CONNECTION_LOG_TAG, "Socket is not connected.");
            return false;
        }
        if (mSocket.isClosed())
        {
            Logger.verbose(Logger.A2DM_CONNECTION_LOG_TAG, "Socket is closed.");
            return false;
        }
        long l = System.currentTimeMillis() - this.mLastSocketActivity.get();
        if (l > MAX_KEEP_ALIVE_INTERVAL) return true;
        Logger.verbose(Logger.A2DM_CONNECTION_LOG_TAG, "Connected to Helium with a healthy socket.");
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
            Logger.warn(Logger.A2DM_CONNECTION_LOG_TAG, "Error closing socket.");
        }
    }

    /**
     * @TODO The socket http://en.wikipedia.org/wiki/Chunked_transfer_encoding
     * @param stream
     * @return
     */
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
            // jsonData = new JSONObject(buffer.toString());

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return jsonData;
    }
}
