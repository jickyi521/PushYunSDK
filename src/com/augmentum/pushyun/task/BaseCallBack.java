package com.augmentum.pushyun.task;

import com.augmentum.pushyun.common.PushException;

public abstract class BaseCallBack implements IFeedBackSink<Void>
{
    public abstract void done(PushException paramParseException);

    public final void internalDone(Void returnValue, PushException e)
    {
        done(e);
    }
}
