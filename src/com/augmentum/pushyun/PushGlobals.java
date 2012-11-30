package com.augmentum.pushyun;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;


/**
 * Global resources for Ignition.
*/
public class PushGlobals
{
    private static final String LOG_TAG = "PushGlobals";
    
    /**
     * Base URL of the Demo Server (such as http://10.0.2.2:8080/gcm-demo)
     */
    //get 
    //udid/android id, app/developer id udid=%s% & app= %s%
    public static final String A2DM_SERVER_REGISTER_URL = "http://192.168.196.58:3000/api/reg";
    //token=xxxx&app=xxxx
    public static final String A2DM_SERVER_MESSAGE_URL = "http://192.168.196.58:3000/api/message";
    //Pre to look up
    public static final String A2DM_SERVER_LOOK_UP_URL = "http://192.168.196.58:3000";
    
    public static final String A2DM_SERVER_HOST = "192.168.196.58";
    public static final int A2DM_SERVER_PORT = 3000;
    
    
    //post
    //appkey, token, name, version
    public static final String CMS_SERVER_REGISTER_URL = "http://192.168.196.58/api.php?op=push_mobile&a=register";
    //reg_id
    public static final String CMS_SERVER_FEEDBACK_URL = "http://192.168.196.58/api.php?op=push_mobile&a=feedback";
    //appkey, version, channel
    public static final String CMS_SERVER_CHANNEL_URL = "http://192.168.196.58/api.php?op=push_mobile&a=channel";

    /**
     * Google API project id registered to use GCM. //487820657755
     * GCM developer project id
     */
    public static final String SENDER_ID = "179344231922";
    
    /**
     * Intent used to display a message in the screen.
     */
    public static final String DISPLAY_MESSAGE_ACTION = "com.augmentum.pushyun.DEBUG_MESSAGE";
    public static final String A2DM_REGISTER_SUCCESS_ACTION = "com.augmentum.pushyun.a2dm.intent.REGISTRATION";

    public static final int GET_METHOD = 0;
    public static final int POST_METHOD = 1;
    
    /**
     * Intent's extra that contains the message to be displayed.
     */
    public static final String EXTRA_MESSAGE = "message";
    
    private static PushGlobals mPushGlobals = null;
    
    private boolean mA2DMServiceStarted = false;
    private boolean mGCMEnabled = true;
    private boolean mGCMChecked = false;
    private boolean mGCMAvailabe = false;
    private boolean mRegisterInGCM = true;
    private String mAppMsgIntentServiceClassPath = "";
    
    private String mAppKey = "";
    
    private PushGlobals()
    {
        
    }
    
    public synchronized static PushGlobals getInstance()
    {
        if(mPushGlobals == null)
        {
            mPushGlobals = new PushGlobals();
        }
        return mPushGlobals;
    }
    
    public boolean isA2DMServiceStarted()
    {
        return mA2DMServiceStarted;
    }

    public void setA2DMServiceStarted(boolean a2dmServiceStarted)
    {
        mA2DMServiceStarted = a2dmServiceStarted;
    }
    
    public boolean isGCMChecked()
    {
        return mGCMChecked;
    }

    public void setGCMChecked(boolean gcmChecked)
    {
        mGCMChecked = gcmChecked;
    }
    
    public boolean isGCMAvailabe()
    {
        return mGCMAvailabe;
    }

    public void setGCMAvailabe(boolean gcmAvailabe)
    {
        setGCMChecked(true);
        mGCMAvailabe = gcmAvailabe;
    }
    
    public boolean isGCMEnabled()
    {
        return mGCMEnabled;
    }

    public void setGCMEnabled(boolean gcmEnabled)
    {
        mGCMEnabled = gcmEnabled;
    }
    
    public boolean isRegisterInGCM()
    {
        return mRegisterInGCM;
    }

    public void setRegisterInGCM(boolean gcm)
    {
        this.mRegisterInGCM = gcm;
    }
    
    //TODO  assets/pushconfig.properties
    public static void parsePushConfig()
    {
        
    }
    
    public String getAppMsgIntentServiceClassPath()
    {
        return mAppMsgIntentServiceClassPath;
    }

    public void setAppMsgIntentServiceClassPath(String appMsgIntentServiceClassPath)
    {
        mAppMsgIntentServiceClassPath = appMsgIntentServiceClassPath;
    }
    
    public String getAppKey()
    {
        return mAppKey;
    }

    public void setAppKey(String appKey)
    {
        mAppKey = appKey;
    }
    
    public void registerDebugMsgReceiver(Context context)
    {
        context.registerReceiver(mHandleMessageReceiver, new IntentFilter(DISPLAY_MESSAGE_ACTION));
    }
    
    public void unRegisterDebugMsgReceiver(Context context)
    {
        context.unregisterReceiver(mHandleMessageReceiver);
    }
    
    /**
     * Notifies UI to display a message.
     * <p>
     * This method is defined in the common helper because it's used both by the UI and the
     * background service.
     * 
     * @param context application's context.
     * @param action  he Intent action.
     * @param message message to be displayed.
     */
    public static void sendPushBroadcast(Context context, String action, String message)
    {
        if(DISPLAY_MESSAGE_ACTION.equals(action))
        {
            Intent intent = new Intent(DISPLAY_MESSAGE_ACTION);
            intent.putExtra(EXTRA_MESSAGE, message);
            context.sendBroadcast(intent);
        }
        else if(A2DM_REGISTER_SUCCESS_ACTION.equals(action))
        {
            Intent intent = new Intent(A2DM_REGISTER_SUCCESS_ACTION);
            intent.putExtra("registration_id", message);
            context.sendOrderedBroadcast(intent, "com.google.android.c2dm.permission.SEND");
        }
    }
    
    
    /**
     * Handle receiver message.
     */
    private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String msg = intent.getExtras().getString(EXTRA_MESSAGE);
            Log.v(LOG_TAG, "************msg**************" + msg);
        }
    };

}