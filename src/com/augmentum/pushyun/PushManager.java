package com.augmentum.pushyun;

import java.util.Random;

import org.apache.http.client.methods.HttpRequestBase;

import com.augmentum.pushyun.common.Logger;
import com.augmentum.pushyun.common.PushException;
import com.augmentum.pushyun.http.Get;
import com.augmentum.pushyun.http.HttpParams;
import com.augmentum.pushyun.http.Post;
import com.augmentum.pushyun.http.response.BaseResponse;
import com.augmentum.pushyun.task.BaseAsyncTask;
import com.augmentum.pushyun.task.HttpCallBack;


public class PushManager
{
    private static final int MAX_ATTEMPTS = 5;
    private static final int BACKOFF_MILLI_SECONDS = 2000;

    private static final Random random = new Random();


    public static synchronized void executeHttpRequest(final HttpRequestBase httpRequestBase, final int method, HttpCallBack callback)
    {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        BaseAsyncTask<Void> httpRequestTask = new BaseAsyncTask(callback)
        {
            BaseResponse response;

            @Override
            public BaseResponse run() throws PushException
            {
                long backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);

                for (int i = 1; i <= MAX_ATTEMPTS; i++)
                {
                    if (method == HttpParams.GET_METHOD)
                    {
                        Get get = (Get)httpRequestBase;
                        response = get.execute();
                    }
                    else if (method == HttpParams.POST_METHOD)
                    {
                        Post post = (Post)httpRequestBase;
                        response = post.execute();
                    }

                    // Success
                    if (response != null && response.isStatusOk())
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
                            Logger.verbose(Logger.HTTP_LOG_TAG, "Sleeping for " + backoff + " ms before, retry to connect" + httpRequestBase.getURI());
                            Thread.sleep(backoff);
                        }
                        catch (InterruptedException e)
                        {
                            // Activity finished before we complete - exit.
                            Logger.verbose(Logger.HTTP_LOG_TAG, "Thread interrupted: abort remaining retries!");
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
