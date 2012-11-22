package com.augmentum.pushyun.common;

import com.augmentum.pushyun.task.BaseAsyncTask;
import com.augmentum.pushyun.task.BaseCallBack;

public class PushUser
{

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
}
