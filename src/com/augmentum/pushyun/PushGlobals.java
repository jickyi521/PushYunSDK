package com.augmentum.pushyun;

import android.content.Context;
import android.content.Intent;


/**
 * Global resources for Ignition.
*/
public class PushGlobals
{
    /**
     * Base URL of the Demo Server (such as http://10.0.2.2:8080/gcm-demo)
     */
    //get 
    //udid/android id, app/developer id udid=%s% & app= %s%
    public static final String A2DM_SERVER_REGISTER_URL = "http://192.168.196.58:3000/api/reg?";
    //token=xxxx&app=xxxx
    public static final String A2DM_SERVER_MESSAGE_URL = "http://192.168.196.58:3000/api/message?";
    
    //post
    //appkey, token, name, version
    public static final String CMS_SERVER_REGISTER_URL = "http://192.168.196.58/api.php?op=push_mobile&a=register";
    //reg_id
    public static final String CMS_SERVER_FEEDBACK_URL = "http://192.168.196.58/api.php?op=push_mobile&a=feedback";
    //appkey, version, channel
    public static final String CMS_SERVER_CHANNEL_URL = "http://192.168.196.58/api.php?op=push_mobile&a=channel";

    /**
     * Google API project id registered to use GCM.
     */
    public static final String SENDER_ID = "487820657755";

    /**
     * Tag used on log messages.
     */
    public static final String TAG = "GCM Mode";

    /**
     * Intent used to display a message in the screen.
     */
    public static final String DISPLAY_MESSAGE_ACTION = "com.google.android.gcm.demo.app.DISPLAY_MESSAGE";

    /**
     * Intent's extra that contains the message to be displayed.
     */
    public static final String EXTRA_MESSAGE = "message";
    
    private static PushGlobals mPushGlobals = null;
    
    private boolean mA2DMServiceStarted = false;
    private boolean mGCMChecked = false;
    private boolean mGCMAvailabe = false;
    
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
    
    /**
     * Notifies UI to display a message.
     * <p>
     * This method is defined in the common helper because it's used both by the UI and the
     * background service.
     * 
     * @param context application's context.
     * @param message message to be displayed.
     */
    public static void displayMessage(Context context, String message)
    {
        Intent intent = new Intent(DISPLAY_MESSAGE_ACTION);
        intent.putExtra(EXTRA_MESSAGE, message);
        context.sendBroadcast(intent);
    }

}