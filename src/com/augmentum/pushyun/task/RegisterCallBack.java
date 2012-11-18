package com.augmentum.pushyun.task;

import com.augmentum.pushyun.common.PushException;

public abstract class RegisterCallBack extends PushCallBack<Void>
{
    public abstract void done(PushException paramParseException);

    final void internalDone(Void returnValue, PushException e)
    {
        done(e);
    }
}
