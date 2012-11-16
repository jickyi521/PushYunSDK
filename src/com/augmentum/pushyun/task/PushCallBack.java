package com.augmentum.pushyun.task;

import com.augmentum.pushyun.PushException;

public abstract class PushCallBack<T>
{
    abstract void internalDone(T paramT, PushException paramParseException);
}
