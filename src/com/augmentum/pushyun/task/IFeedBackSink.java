package com.augmentum.pushyun.task;

import com.augmentum.pushyun.common.PushException;

/**
 * General-purpose callback interface.
 */
public interface IFeedBackSink<T>
{
    /**
     * 
     * @param paramT 
     * @param paramParseException
     */
    void internalDone(T paramT, PushException paramParseException);
}
