package com.augmentum.pushyun.http;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.augmentum.pushyun.PushGlobals;
import com.augmentum.pushyun.common.Logger;

public class NetWorkInfo
{

    private static ConnectivityManager connMan()
    {
        return (ConnectivityManager)PushGlobals.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    private static NetworkInfo info()
    {
        ConnectivityManager localConnectivityManager = connMan();
        if (localConnectivityManager != null) return localConnectivityManager.getActiveNetworkInfo();
        Logger.error(Logger.HTTP_LOG_TAG, "Error fetching network info.");
        return null;
    }

    public static boolean isConnected()
    {
        NetworkInfo localNetworkInfo = info();
        if (localNetworkInfo == null) return false;
        return localNetworkInfo.isConnected();
    }

    public static String typeName(Context context)
    {
        NetworkInfo localNetworkInfo = info();
        if (localNetworkInfo == null) return "none";
        return localNetworkInfo.getTypeName();
    }

    public static String getActiveIPAddress()
    {
        String str = null;
        try
        {
            Enumeration<?> localEnumeration1 = null;
            for (int i = 0; (localEnumeration1 == null) && (i < 2); i++)
                try
                {
                    localEnumeration1 = NetworkInterface.getNetworkInterfaces();
                }
                catch (NullPointerException localNullPointerException)
                {
                    Logger.debug(Logger.HTTP_LOG_TAG, "NetworkInterface.getNetworkInterfaces failed with exception (ICS Bug): "
                            + localNullPointerException.toString());
                }
            if (localEnumeration1 == null)
            {
                Logger.debug(Logger.HTTP_LOG_TAG, "No network interfaces currently available.");
                return null;
            }
            while (localEnumeration1.hasMoreElements())
            {
                NetworkInterface localNetworkInterface = (NetworkInterface)localEnumeration1.nextElement();
                Enumeration<?> localEnumeration2 = localNetworkInterface.getInetAddresses();
                while (localEnumeration2.hasMoreElements())
                {
                    InetAddress localInetAddress = (InetAddress)localEnumeration2.nextElement();
                    if ((!localInetAddress.isLoopbackAddress()) && (str == null)) str = localInetAddress.getHostAddress();
                }
            }
        }
        catch (SocketException localSocketException)
        {
            Logger.error(Logger.HTTP_LOG_TAG, "Error fetching IP address information");
        }
        Logger.verbose(Logger.HTTP_LOG_TAG, "Detected active IP address as: " + str);
        return str;
    }
}