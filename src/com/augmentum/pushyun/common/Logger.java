package com.augmentum.pushyun.common;

import android.util.Log;

public class Logger
{
    public static int mLogLevel = 6;
    public static String LOG_TAG = "PushYunSDK";

    public static void warn(String paramString)
    {
      if (mLogLevel <= 5)
        Log.w(LOG_TAG, paramString);
    }

    public static void warn(String paramString, Throwable paramThrowable)
    {
      if (mLogLevel <= 5)
        Log.w(LOG_TAG, paramString, paramThrowable);
    }

    public static void warn(Throwable paramThrowable)
    {
      if (mLogLevel <= 5)
        Log.w(LOG_TAG, paramThrowable);
    }

    public static void verbose(String paramString)
    {
      if (mLogLevel <= 2)
        Log.v(LOG_TAG, paramString);
    }

    public static void debug(String paramString)
    {
      if (mLogLevel <= 3)
        Log.d(LOG_TAG, paramString);
    }

    public static void info(String paramString)
    {
      if (mLogLevel <= 4)
        Log.i(LOG_TAG, paramString);
    }

    public static void info(String paramString, Throwable paramThrowable)
    {
      if (mLogLevel <= 4)
        Log.i(LOG_TAG, paramString, paramThrowable);
    }

    public static void error(String paramString)
    {
      if (mLogLevel <= 6)
        Log.e(LOG_TAG, paramString);
    }

    public static void error(Throwable paramThrowable)
    {
      if (mLogLevel <= 6)
        Log.e(LOG_TAG, null, paramThrowable);
    }

    public static void error(String paramString, Throwable paramThrowable)
    {
      if (mLogLevel <= 6)
        Log.e(LOG_TAG, paramString, paramThrowable);
    }
}
