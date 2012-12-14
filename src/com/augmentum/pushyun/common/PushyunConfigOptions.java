package com.augmentum.pushyun.common;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;

import com.augmentum.pushyun.util.StrUtils;

public class PushyunConfigOptions
{
    private static PushyunConfigOptions mPushyunConfigOptions;

    private String mAPIKey = "andriodsdk1.0";
    private String mAPISecret = "7ecfc6e6c4a8ab28fcf3a41d241c3888";
    private String mChannel = "";

    private String mGCMAppKey;
    private String mA2DMAppKey;
    private String mAPPIntentServicePath = "com.augmentum.pushyun.service.PushMsgIntentService"; // default
    private int mTransportType;
    private boolean mDebugSwitch = false;

    private TransportType mTransportStyle = TransportType.HYBRID;

    private PushyunConfigOptions()
    {

    }

    public static PushyunConfigOptions getInstance()
    {
        if (mPushyunConfigOptions == null)
        {
            mPushyunConfigOptions = new PushyunConfigOptions();
        }
        return mPushyunConfigOptions;
    }

    public String getAPIKey()
    {
        return mAPIKey;
    }

    public String getAPISecret()
    {
        return mAPISecret;
    }

    public String getChannel()
    {
        return mChannel;
    }

    public String getGCMAppKey()
    {
        return mGCMAppKey;
    }

    public String getA2DMAppKey()
    {
        return mA2DMAppKey;
    }

    public String getAppIntentServicePath()
    {
        return mAPPIntentServicePath;
    }

    public TransportType getTransportType()
    {
        return mTransportStyle;
    }

    public void loadPushyunConfigOptions(Intent intent)
    {
        if (!StrUtils.isEmpty(intent.getStringExtra("mGCMAppKey")))
        {
            mGCMAppKey = intent.getStringExtra("mGCMAppKey");
        }
        if (!StrUtils.isEmpty(intent.getStringExtra("mA2DMAppKey")))
        {
            mA2DMAppKey = intent.getStringExtra("mA2DMAppKey");
        }
        if (!StrUtils.isEmpty(intent.getStringExtra("mAPPIntentServicePath")))
        {
            mAPPIntentServicePath = intent.getStringExtra("mAPPIntentServicePath");
        }
        if (!StrUtils.isEmpty(intent.getStringExtra("mTransportType")))
        {
            setTransportType(intent.getIntExtra("mTransportType", 2));
        }

        mDebugSwitch = intent.getBooleanExtra("mDebugSwitch", false);
        Logger.mLogSwitch = mDebugSwitch;
    }

    public void loadPushyunConfigOptions(Context context)
    {
        loadFromProperties(context, "pushconfig.properties");
    }

    public enum TransportType
    {
        GCM, A2DM, HYBRID;
    }

    private void setTransportType(int type)
    {
        switch (type)
        {
            case 0:
                mTransportStyle = TransportType.GCM;
                break;
            case 1:
                mTransportStyle = TransportType.A2DM;
                break;
            case 2:
                mTransportStyle = TransportType.HYBRID;
                break;
        }
    }

    private void loadFromProperties(Context context, String properiesPath)
    {
        Resources localResources = context.getResources();
        AssetManager localAssetManager = localResources.getAssets();
        try
        {
            if (!Arrays.asList(localAssetManager.list("")).contains(properiesPath))
            {
                Logger.verbose(Logger.OTHERS_LOG_TAG, "Options - Couldn't find " + properiesPath);
                return;
            }
        }
        catch (IOException localIOException1)
        {
            Logger.error(Logger.OTHERS_LOG_TAG, localIOException1);
            return;
        }

        Properties localProperties = new Properties();
        try
        {
            InputStream localInputStream = localAssetManager.open(properiesPath);
            localProperties.load(localInputStream);
            Class<? extends PushyunConfigOptions> localClass = getClass();
            List<Field> localList = Arrays.asList(localClass.getDeclaredFields());
            ListIterator<Field> localListIterator = localList.listIterator();
            while (localListIterator.hasNext())
            {
                Field localField = (Field)localListIterator.next();
                if (!PushyunConfigOptions.class.isAssignableFrom(localField.getType()))
                {
                    String str = localProperties.getProperty(localField.getName());
                    if (str != null)
                        try
                        {
                            if ((localField.getType() == Boolean.TYPE) || (localField.getType() == Boolean.class))
                            {
                                localField.set(this, Boolean.valueOf(str));
                                if (localField.getName().equals("mDebugSwitch"))
                                {
                                    Logger.mLogSwitch = Boolean.valueOf(str);
                                }
                            }
                            else if ((localField.getType() == Integer.TYPE) || (localField.getType() == Integer.class))
                            {
                                localField.set(this, Integer.valueOf(str));
                                if (localField.getName().equals("mTransportType"))
                                {
                                    setTransportType(mTransportType);
                                }
                            }
                            else if ((localField.getType() == Long.TYPE) || (localField.getType() == Long.class)) localField.set(this,
                                    Long.valueOf(str));
                            else try
                            {
                                localField.set(this, str.trim());
                            }
                            catch (IllegalArgumentException localIllegalArgumentException)
                            {
                                Logger.error(Logger.OTHERS_LOG_TAG, "Unable to set field '" + localField.getName()
                                        + "' due to type mismatch.");
                            }
                        }
                        catch (IllegalAccessException localIllegalAccessException)
                        {
                            Logger.error(Logger.OTHERS_LOG_TAG, "Unable to set field '" + localField.getName()
                                    + "' because the field is not visible.");
                        }
                }
            }
        }
        catch (IOException localIOException2)
        {
            Logger.error("Error loading properties file " + properiesPath, localIOException2);
        }
    }
}
