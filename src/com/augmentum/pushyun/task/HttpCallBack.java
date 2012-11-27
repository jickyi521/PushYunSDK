package com.augmentum.pushyun.task;

import com.augmentum.pushyun.common.PushException;
import com.augmentum.pushyun.http.response.BaseResponse;

public abstract class HttpCallBack implements IFeedBackSink<BaseResponse>
{
    public abstract void done(BaseResponse respone, PushException e);

    public final void internalDone(BaseResponse respone, PushException e)
    {
        done(respone, e);
    }
}
