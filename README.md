# 分享快速集成
## 1.产品概述

群硕社会化组件，可以让移动应用快速具备社会化分享、登录等功能，并提供实时、全面的社会化数据统计分析服务。

指南将会手把手教你使用社会化组件SDK，用5分钟为APP增加新浪微博、腾讯微博、人人网分享功能。

注意：本文示例代码均针对最新版SDK，如果你所用SDK的类名或方法名与此文不符合，请使用你所用SDK的随包文档、或者下载使用最新版SDK。

## 2.配置AndroidManifest.xml

 步骤一：声明SDK权限

    
        <!-- ###################声明SDK使用的相关权限###################### -->
        <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />  <!-- 检测网络状态 -->
        <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />     <!-- 获取mac地址作为用户的备用唯一标识 -->
        <uses-permission android:name="android.permission.READ_PHONE_STATE" />      <!-- 获取用户手机的IMEI，用来唯一的标识用户。 -->
        <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" /><!-- 缓存资源优先存入SDcard -->
        <uses-permission android:name="android.permission.INTERNET" />              <!-- 允许应用程序联网，以便向我们的服务器端发送数据。 -->
        <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
        <uses-permission android:name="android.permission.GET_ACCOUNTS" />
        <uses-permission android:name="android.permission.MANAGE_ACCOUNTS" />

步骤二：声明需要使用的Activity

    <activity
        android:name="com.tencent.tauth.AuthActivity"
        android:launchMode="singleTask"
        android:noHistory="true" >
        <intent-filter>
            <action android:name="android.intent.action.VIEW" />
            <category android:name="android.intent.category.DEFAULT" />
            <category android:name="android.intent.category.BROWSABLE" />
            <data android:scheme="tencent100384151" />
        </intent-filter>
    </activity>
     
    <activity android:name="com.augcloud.mobile.socialengine.controller.SnsShareActivity" />
    <activity
        android:name="com.renn.rennsdk.oauth.OAuthActivity"
        android:configChanges="orientation|navigation|keyboardHidden"
        android:screenOrientation="portrait" />
        
    <activity android:name="com.augcloud.mobile.socialengine.view.FriendListActivity" />
    <activity android:name="com.augcloud.mobile.socialengine.view.InviteFriendsActivity" />
    <activity android:name="com.augcloud.mobile.socialengine.view.SnsBridgeActivity" />
    <activity android:name="com.augcloud.mobile.socialengine.view.SnsShareActivity" />
    
  步骤三: 声明augAppId, 群硕应用Id.
  
     <meta-data
            android:name="augAppId"
            android:value="setest" />

## 3.将SDK提供的res文件夹拷入工程目录下, 和工程本身res目录合并。 
提示: SDK提供的资源文件都以 se_开头， 可以更改资源内容但是请不要更改文件名和资源ID


## 4.定义分享app配置信息
在工程assets目录下，按照以下约定新建所需要平台信息 sns_app_config.xml，填入相应的平台申请信息。

<?xml version="1.0" encoding="UTF-8"?>
<resources>

    <!--
    appId: 识别平台的Id（不可更改） 
    sortId: 在popup 分享面板上各平台排列顺序
    description: 平台的描述信息
    logo: 平台的logo(不可更改)
    appKey: 您所申请当前应用的key
    appSecret: 您所申请当前应用的secret
    apiKey: 您所申请当前应用的apiKey
    type: 1000: 新浪微博 ,  1001: 腾讯微博,  1002: QQ空间,  1003: 微信好友,
          1004: 微信朋友圈 , 1005: 微信朋友圈,  1005: QQ,  1006: 人人网,
          1007: 豆瓣, 1008: 邮件,  1009: 信息,
    enable: applied or not

    -->
    <apps>
        <app appId="1000" sortId="1" description="新浪微博" enable="1" logo="se_sinaweibo_logo" appKey="259956172" appSecret="ca07b07dfcf9fcea18b3fbb88238ff63" apiKey=""  redirectURI="" />
        <app appId="1001" sortId="2" description="腾讯微博" enable="1" logo="se_tencentweibo_logo"  appKey="801318726" appSecret="7126bcd3c992ec4dd3bbb7881e981639" apiKey=""  redirectURI=""/>
        <app appId="1002" sortId="3" description="QQ空间" enable="1" logo="se_qzone_logo"  appKey="" appSecret="" apiKey=""  redirectURI=""/>
        <app appId="1003" sortId="4" description="微信好友" enable="1" logo="se_wechat_logo"  appKey="wxab5705a12c71265a" appSecret="" apiKey=""  redirectURI=""/>
        <app appId="1004" sortId="5" description="微信朋友圈" enable="1" logo="se_wechat_circle_logo"  appKey="wxab5705a12c71265a" appSecret="" apiKey=""  redirectURI=""/>
        <app appId="1005" sortId="6" description="QQ" enable="1" logo="se_qq_logo"  appKey="100384151" appSecret="" apiKey=""  redirectURI=""/>
        <app appId="1006" sortId="7" description="人人网" enable="1" logo="se_renren_logo"  appKey="219081" appSecret="f7f5eae42cd2485c92ef717b306b5df9" apiKey="a9dd14d7cf69404fad286872a340a150"  redirectURI=""/>
        <app appId="1007" sortId="8" description="豆瓣" enable="1" logo="se_douban_logo"  appKey="08520d2ca183848518f62785e48a8d8f" appSecret="c40662b75e453f1b" apiKey=""  redirectURI=""/>
        <app appId="1008" sortId="9" description="邮件" enable="1" logo="se_email_logo"  appKey="" appSecret="" apiKey=""  redirectURI=""/>
        <app appId="1009" sortId="10" description="信息" enable="1" logo="se_message_logo"  appKey="" appSecret="" apiKey=""  redirectURI=""/>
    </apps>

</resources>


## 4.新项目Application onCreate中调用SDK的初始化代码

    /**
     * @param context Application context
     * @param configXmlPath App logo configuration should be located assets package, e,g. logo_global_config.xml
     */
    AugSnsSDK.init(Application context, String configXmlPath)
    
## 5.打开popup 分享平台选择面板

    /**
     * @param context, Activity context
     * @param snsInfo, Share info @SnsInfo 
     */
    AugSnsSDK.openShare(Activity context, SnsInfo snsInfo)

*********


## 6.群硕SnsSDK API：

+ 授权
 
        /**
           * @param context, Activity context
           * @param platformKey, Sns app type, it defined in @PlatForm
           * @param snsSsoListener, Feedback share results
           */
        AugSnsSDK.doSnsOauthVerify(Activity context, String platformKey, SnsSsoListener snsSsoListener) 
        
+ 分享
 
        /**
           * @param context Activity， context
           * @param platformKey, Sns app type, it defined in @PlatForm
           * @param snsInfo, Share info @SnsInfo 
           * @param directly, Weather need to open sns share screen,  you can edit content in the screen if it's true
           * @param snsSsoListener, Feedback share results
           */
        AugSnsSDK.shareToSns(Activity context, String platformKey, SnsInfo snsInfo, boolean directly, SnsSsoListener snsSsoListener)

+ 邀请

        /**
           * @param context, Activity context
           * @param platformKey, Sns app type, it defined in @PlatForm
           * @param snsInfo, Invite info @SnsInfo 
           */
        AugSnsSDK.inviteToSns(Activity context, String platformKey, SnsInfo snsInfo)
