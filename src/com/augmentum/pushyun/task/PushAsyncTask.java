package com.augmentum.pushyun.task;

import android.os.AsyncTask;

import com.augmentum.pushyun.common.PushException;

public abstract class PushAsyncTask<T> extends AsyncTask<Void, Void, Void>
{
    private PushCallBack<T> mCallBack;
    private T mResult;
    private PushException mException;

    public PushAsyncTask(PushCallBack<T> theCallback)
    {
        this.mResult = null;
        this.mException = null;
        this.mCallBack = theCallback;
    }

    public abstract T run() throws PushException;

    @Override
    protected Void doInBackground(Void... params)
    {
        try
        {
            this.mResult = run();
            return null;
        }
        catch (PushException e)
        {
            this.mException = e;
        }
        return null;
    }

    protected void onPostExecute(Void v)
    {
        if (this.mCallBack != null) this.mCallBack.internalDone(this.mResult, this.mException);
    }

    void executeInThisThread()
    {
        doInBackground(new Void[0]);
        onPostExecute(null);
    }

    public static int executeTask(PushAsyncTask<?> task)
    {
        task.execute(new Void[0]);
        return 0;
    }

}
