package com.augmentum.pushyun.common;

import android.util.Log;

public class Logger
{
    public static final String LOG_TAG = "PushYunSDK";
    public static final String SERVICE_LOG_TAG = "Service";
    public static final String HTTP_LOG_TAG = "HTTP";
    public static final String GCM_LOG_TAG = "GCM";
    public static final String A2DM_LOG_TAG = "A2DM";
    public static final String A2DM_CONNECTION_LOG_TAG = "A2DMConnection";
    public static final String MESSAGE_LOG_TAG = "Message";
    public static final String NOTIFICATION_LOG_TAG = "Notification";
    public static final String RECEIVER_LOG_TAG = "Receiver";
    public static final String OTHERS_LOG_TAG = "Others";

    public static int mLogLevel = 6;
    public static boolean mLogSwitch = false;

    public static void warn(String tag, String msg)
    {
        if (mLogLevel <= 5 && mLogSwitch) Log.w(tag, msg);
    }

    public static void warn(String tag, String msg, Throwable paramThrowable)
    {
        if (mLogLevel <= 5 && mLogSwitch) Log.w(tag, msg, paramThrowable);
    }

    public static void warn(String tag, Throwable paramThrowable)
    {
        if (mLogLevel <= 5 && mLogSwitch) Log.w(tag, paramThrowable);
    }

    public static void verbose(String tag, String msg)
    {
        if (mLogLevel <= 2 && mLogSwitch) Log.v(tag, msg);
    }

    public static void debug(String tag, String msg)
    {
        if (mLogLevel <= 3 && mLogSwitch) Log.d(tag, msg);
    }

    public static void info(String tag, String msg)
    {
        if (mLogLevel <= 4 && mLogSwitch) Log.i(tag, msg);
    }

    public static void info(String tag, String msg, Throwable paramThrowable)
    {
        if (mLogLevel <= 4 && mLogSwitch) Log.i(tag, msg, paramThrowable);
    }

    public static void error(String tag, String msg)
    {
        if (mLogLevel <= 6 && mLogSwitch) Log.e(tag, msg);
    }

    public static void error(String tag, Throwable paramThrowable)
    {
        if (mLogLevel <= 6 && mLogSwitch) Log.e(tag, null, paramThrowable);
    }

    public static void error(String tag, String msg, Throwable paramThrowable)
    {
        if (mLogLevel <= 6 && mLogSwitch) Log.e(tag, msg, paramThrowable);
    }
}
