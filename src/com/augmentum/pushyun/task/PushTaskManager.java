package com.augmentum.pushyun.task;

import java.util.Random;

import org.apache.http.client.methods.HttpRequestBase;

import android.content.Context;
import android.util.Log;

import com.augmentum.pushyun.PushGlobals;
import com.augmentum.pushyun.common.PushException;
import com.augmentum.pushyun.http.Get;
import com.augmentum.pushyun.http.Post;
import com.augmentum.pushyun.http.RegisterRequest;
import com.augmentum.pushyun.http.response.BaseResponse;

public class PushTaskManager
{
    private static final String LOG_TAG = "PushTaskManager";
    
    private static final int MAX_ATTEMPTS = 5;
    private static final int BACKOFF_MILLI_SECONDS = 2000;
    
    private static final Random random = new Random();
    
    /**
     * Implement the RegisterCallBack, and
     * 
     * @param callback
     */
    public void registerInBackground(BaseCallBack callback)
    {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        BaseAsyncTask registerTask = new BaseAsyncTask(callback)
        {
            public Void run() throws PushException
            {
                // ParseUser.this.signUp(false);
                return null;
            }
        };
        BaseAsyncTask.executeTask(registerTask);
    }

    /**
     * 
     * @param context
     * @param regId
     * @param callback
     */
    public static void registerInCMSBackground(final Context context, final String regId, BaseCallBack callback)
    {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        BaseAsyncTask registerCMSTask = new BaseAsyncTask(callback)
        {
            @Override
            public Boolean run() throws PushException
            {
                return RegisterRequest.registerCMSServer(context, regId);
            }
        };

        BaseAsyncTask.executeTask(registerCMSTask);
    }

    public static void executeHttpRequest(final HttpRequestBase httpRequestBase, final int method, HttpCallBack callback)
    {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        BaseAsyncTask<Void> httpRequestTask = new BaseAsyncTask(callback)
        {
            BaseResponse response;
            @Override
            public BaseResponse run() throws PushException
            {
                long backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);
                
                for(int i = 1; i<= MAX_ATTEMPTS; i++)
                {
                    if(method == PushGlobals.GET_METHOD)
                    {
                        Get get = (Get)httpRequestBase;
                        response = get.execute();
                    }
                    else if(method == PushGlobals.POST_METHOD)
                    {
                        Post post = (Post)httpRequestBase;
                        response = post.execute();
                    }
                    
                    //Success
                    if(response.status() == 200)
                    {
                        return response;
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
                        catch (InterruptedException e)
                        {
                            // Activity finished before we complete - exit.
                            Log.d(LOG_TAG, "Thread interrupted: abort remaining retries!");
                            Thread.currentThread().interrupt();
                            return response;
                        }
                        // increase backoff exponentially
                        backoff *= 2;
                    }
                }
                return response;
            }
        };
        
        BaseAsyncTask.executeTask(httpRequestTask);
    }
}
