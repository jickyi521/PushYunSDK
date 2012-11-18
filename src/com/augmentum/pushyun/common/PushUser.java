package com.augmentum.pushyun.common;

import com.augmentum.pushyun.task.PushAsyncTask;
import com.augmentum.pushyun.task.RegisterCallBack;

public class PushUser
{

    /**
     * Implement the RegisterCallBack, and 
     * 
     * @param callback
     */
    public void registerInBackground(RegisterCallBack callback)
    {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        PushAsyncTask registerTask = new PushAsyncTask(callback)
        {
            public Void run() throws PushException
            {
                // ParseUser.this.signUp(false);
                return null;
            }
        };
        PushAsyncTask.executeTask(registerTask);
    }
}
