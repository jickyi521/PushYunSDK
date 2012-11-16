package com.augmentum.pushyun;

/**
 * 
 * The base of push exception
 * TODO
 */
public class PushException extends Exception
{
    private static final long serialVersionUID = 1L;
    private int mCode;
    
    public PushException(int theCode, String theMessage)
    {
      super(theMessage);
      this.mCode = theCode;
    }

    public PushException(String message, Throwable cause)
    {
      super(message, cause);
      this.mCode = -1;
    }

    public PushException(Throwable cause)
    {
      super(cause);
      this.mCode = -1;
    }

    public int getCode()
    {
      return this.mCode;
    }
}
