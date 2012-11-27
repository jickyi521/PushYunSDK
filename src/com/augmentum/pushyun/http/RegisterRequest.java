package com.augmentum.pushyun.http;

import static com.augmentum.pushyun.PushGlobals.CMS_SERVER_REGISTER_URL;
import static com.augmentum.pushyun.PushGlobals.DISPLAY_MESSAGE_ACTION;
import static com.augmentum.pushyun.PushGlobals.sendPushBroadcast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.util.Log;

import com.augmentum.pushyun.register.RegisterManager;

/**
 * Helper class used to communicate with the demo server.
 */
public final class RegisterRequest
{
    private static final String LOG_TAG = "RegisterRequest";

    private static final int MAX_ATTEMPTS = 5;
    private static final int BACKOFF_MILLI_SECONDS = 2000;
    private static final Random random = new Random();

    public static final int STATUS_OK = 200; // 200 request success

    /**
     * Register this account/device pair within the server.
     * 
     * @return whether the registration succeeded or not.
     */
    public static boolean registerCMSServer(final Context context, final String regId)
    {
        Log.i(LOG_TAG, "registering device (regId = " + regId + ")");

        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("appkey", "com.push.test"));
        nameValuePairs.add(new BasicNameValuePair("token", "ffffffff-8b25-33fb-e812-480b62ffe7ff"));
        nameValuePairs.add(new BasicNameValuePair("name", "Test"));
        nameValuePairs.add(new BasicNameValuePair("version", "1"));

        long backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);
        // Once GCM returns a registration id, we need to register it in the
        // demo server. As the server might be down, we will retry it a couple
        // times.
        for (int i = 1; i <= MAX_ATTEMPTS; i++)
        {
            Log.d(LOG_TAG, "Attempt #" + i + " to register");
            try
            {
                sendPushBroadcast(context, DISPLAY_MESSAGE_ACTION, "Trying (attempt " + i + "/" + MAX_ATTEMPTS + ") to register device on Demo Server.)");

                if (postToCMSServer(CMS_SERVER_REGISTER_URL, nameValuePairs))
                {
                    RegisterManager.setRegisteredOnCMSServer(context, true);
                    String message = "From Demo Server: successfully added device!";
                    sendPushBroadcast(context, DISPLAY_MESSAGE_ACTION, message);
                    return true;
                }
                else
                {
                    if (i == MAX_ATTEMPTS)
                    {
                        break;
                    }
                    try
                    {
                        Log.d(LOG_TAG, "Sleeping for " + backoff + " ms before retry");
                        Thread.sleep(backoff);
                    }
                    catch (InterruptedException e1)
                    {
                        // Activity finished before we complete - exit.
                        Log.d(LOG_TAG, "Thread interrupted: abort remaining retries!");
                        Thread.currentThread().interrupt();
                        return false;
                    }
                    // increase backoff exponentially
                    backoff *= 2;
                }

            }
            catch (Exception e)
            {
                // Here we are simplifying and retrying on any error; in a real
                // application, it should retry only on unrecoverable errors
                // (like HTTP error code 503).
                Log.e(LOG_TAG, "Failed to register on attempt " + i, e);
                if (i == MAX_ATTEMPTS)
                {
                    break;
                }
                try
                {
                    Log.d(LOG_TAG, "Sleeping for " + backoff + " ms before retry");
                    Thread.sleep(backoff);
                }
                catch (InterruptedException e1)
                {
                    // Activity finished before we complete - exit.
                    Log.d(LOG_TAG, "Thread interrupted: abort remaining retries!");
                    Thread.currentThread().interrupt();
                    return false;
                }
                // increase backoff exponentially
                backoff *= 2;
            }
        }
        String message = "Could not register device on Demo Server after " + MAX_ATTEMPTS + " attempts.";
        sendPushBroadcast(context, DISPLAY_MESSAGE_ACTION, message);
        return false;
    }

    /**
     * Unregister this account/device pair within the server.
     */
    public static void unregisterCMSServer(final Context context, final String regId)
    {
        Log.i(LOG_TAG, "unregistering device (regId = " + regId + ")");

        // TODO need to confirm the unregister api
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("appkey", "com.push.test"));
        nameValuePairs.add(new BasicNameValuePair("regId", regId));

        try
        {
            postToCMSServer(CMS_SERVER_REGISTER_URL, nameValuePairs);
            RegisterManager.setRegisteredOnCMSServer(context, false);
            String message = "";
            sendPushBroadcast(context, DISPLAY_MESSAGE_ACTION, message);
        }
        catch (Exception e)
        {
            // At this point the device is unregistered from GCM, but still
            // registered in the server.
            // We could try to unregister again, but it is not necessary:
            // if the server tries to send a message to the device, it will get
            // a "NotRegistered" error message and should unregister the device.
            String message = "Could not unregister device on Demo Server, error message: "+e.getMessage();
            sendPushBroadcast(context, DISPLAY_MESSAGE_ACTION, message);
        }
    }

    /**
     * Issue a POST request to the server.
     * 
     * @param endpoint POST address.
     * @param params request parameters.
     * 
     * @throws IOException propagated from POST.
     */
    private static boolean postToCMSServer(String endpoint, List<NameValuePair> nameValuePairs)
    {
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = null;

        try
        {
            HttpPost httppost = new HttpPost(endpoint);
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            response = httpclient.execute(httppost);
            HttpEntity resEntity = response.getEntity();

            if (resEntity != null)
            {
                Log.v(LOG_TAG, "Response content length: " + resEntity.getContentLength());
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();
        }
        return response != null ? response.getStatusLine().getStatusCode() == STATUS_OK : false;
    }

}