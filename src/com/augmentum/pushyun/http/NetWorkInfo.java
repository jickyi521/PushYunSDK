package com.augmentum.pushyun.http;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.augmentum.pushyun.common.Logger;

public class NetWorkInfo
{
    private static ConnectivityManager connMan(Context context)
    {
        return (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    private static NetworkInfo info(Context context)
    {
        ConnectivityManager localConnectivityManager = connMan(context);
        if (localConnectivityManager != null) return localConnectivityManager.getActiveNetworkInfo();
        Logger.error(Logger.HTTP_LOG_TAG, "Error fetching network info.");
        return null;
    }

    public static boolean isConnected(Context context)
    {
        NetworkInfo localNetworkInfo = info(context);
        if (localNetworkInfo == null) return false;
        return localNetworkInfo.isConnected();
    }

    public static String typeName(Context context)
    {
        NetworkInfo localNetworkInfo = info(context);
        if (localNetworkInfo == null) return "none";
        return localNetworkInfo.getTypeName();
    }
}
