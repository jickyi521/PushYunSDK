package com.augmentum.pushyun.http;

import java.util.TreeMap;

import com.augmentum.pushyun.PushGlobals;
import com.augmentum.pushyun.common.PushyunConfigOptions;
import com.augmentum.pushyun.register.RegisterManager;

public class HttpParams
{
    public static final int A2DM_SERVER_PORT = 3000;
    public static final String A2DM_SERVER_LOOK_UP_URL = "http://192.168.196.58";
    public static final String A2DM_SERVER_REGISTER_URL = "http://192.168.196.58/client/register ";
    public static final String A2DM_SERVER_CONNECTOR_URL = "http://192.168.196.58/client/connector ";

    public static final String CMS_SERVER_REGISTER_URL = "http://192.168.196.58/api/push/register";
    public static final String CMS_SERVER_LAUNCHED_URL = "http://192.168.196.58/api/push/launched";
    public static final String CMS_SERVER_FEEDBACK_URL = "http://192.168.196.58/api/push/feedback";
    public static final String CMS_SERVER_UNREGISTER_CHANNEL_URL = "http://192.168.196.58/api/push/unregister";

    public static final int GET_METHOD = 0;
    public static final int POST_METHOD = 1;

    public static String apiVersion = "apiVersion";
    public static String apiVersionValue = "apiVersionValue";
    public static String platform = "platform";
    public static String platformValue = "android";
    public static String pushPlatform = "pushPlatform";
    public static String pushPlatformValue = PushGlobals.getInstance().isRegisterInGCM() ? "gcm" : "a2dm";
    public static String apiKey = "apiKey";
    public static String apiKeyValue = PushyunConfigOptions.getInstance().getAPIKey();
    public static String appKey = "appKey";
    public static String appKeyValue = PushGlobals.getInstance().isRegisterInGCM() ? PushyunConfigOptions.getInstance().getGCMAppKey()
            : PushyunConfigOptions.getInstance().getA2DMAppKey();
    public static String token = "token";
    public static String tokenValue = RegisterManager.getRegistrationId();
    public static String channel = "channel";
    public static String channelValue = PushyunConfigOptions.getInstance().getChannel();
    public static String appVersion = "appVersion";
    public static String appVersionValue = String.valueOf(PushGlobals.getAppVersion());
    public static String deviceName = "deviceName";
    public static String deviceNameValue = android.os.Build.MODEL;
    public static String udid = "udid";
    public static String udidValue = RegisterManager.generateUDIDValue(PushGlobals.getAppContext());
    public static String nidVersion = "nid";
    public static String sign = "sign";

    /**
     * Common http head
     * 
     * @param apiParamsMap
     */
    public static void generateHttpHead(TreeMap<String, String> apiParamsMap)
    {
        apiParamsMap.put(apiVersion, apiVersionValue);
        apiParamsMap.put(platform, platformValue);
        apiParamsMap.put(pushPlatform, pushPlatformValue);
        apiParamsMap.put(apiKey, apiKeyValue);
        apiParamsMap.put(appKey, appKeyValue);
        apiParamsMap.put(token, tokenValue);
    }

}
