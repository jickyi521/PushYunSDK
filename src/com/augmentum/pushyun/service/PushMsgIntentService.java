package com.augmentum.pushyun.service;

import java.util.HashMap;

import android.content.Context;

import com.augmentum.pushyun.PushGlobals;
import com.augmentum.pushyun.common.Logger;

public class PushMsgIntentService extends CoreMsgIntentService
{

    private static String mMsg = "";
    public static Long mCounter = 0L;

    @Override
    protected void onMessageDelivered(Context context, HashMap<String, String> msgMap)
    {
        mMsg = "***** onMessageDelivered *****message=" +msgMap.get("message");
        mCounter ++ ;
        PushGlobals.sendPushBroadcast(context, PushGlobals.DISPLAY_MESSAGE_ACTION, mMsg);
        Logger.verbose(Logger.SERVICE_LOG_TAG, mMsg);

    }

    @Override
    protected void onError(Context context, String error)
    {
        mMsg = "***** onError *****" +error; 
        PushGlobals.sendPushBroadcast(context, PushGlobals.DISPLAY_MESSAGE_ACTION, mMsg);
        Logger.verbose(Logger.SERVICE_LOG_TAG, mMsg);
    }

    @Override
    protected void onRegistered(Context context, String regId, boolean inGCM)
    {
        //PushGlobals.sendPushBroadcast(context, PushGlobals.DISPLAY_MESSAGE_ACTION, "Registration Id = " +regId);
        Logger.verbose(Logger.SERVICE_LOG_TAG, "Register in GCM" + inGCM + "***** onRegistered *****" + regId);
    }

}