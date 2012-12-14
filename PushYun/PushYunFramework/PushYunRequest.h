//
//  PushYunRequest.h
//  PushYun
//
//  Created by Alvin.Zeng on 12/11/12.
//  Copyright (c) 2012 Alvin.Zeng. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "SVHTTPRequest.h"
#import "PushYunTool.h"

//Define SaaS point
#define LANUNCNED_API_URL @"http://developer.pushyun.com/api/push/launched"
#define FEEDBACK_API_URL @"http://developer.pushyun.com/api/push/feedback"
#define REGISTER_API_URL @"http://developer.pushyun.com/api/push/register"
#define UNREGISTER_API_URL @"http://developer.pushyun.com/api/push/unregister"

#define API_KEY @"123456"
#define API_VERSION @"1"
#define PLATFORM @"ios"
#define PUSH_PLATFORM @"apns"

//Define http params key name, must same with SaaS
#define API_VERSION_NAME @"apiVersion"
#define PLATFORM_NAME @"platform"
#define PUSH_PLATFORM_NAME @"pushPlatform"
#define API_KEY_NAME @"apiKey"
#define APP_KEY_NAME @"appKey"
#define TOKEN_NAME @"token"
#define APP_VERSION_NAME @"appVersion"
#define DEVICE_NAME_NAME @"deviceName"
#define CHANNEL_NAME @"channel"
#define UDID_NAME @"udid"
#define SIGN_NAME @"sign"
#define NID_NAME @"nid"

@interface PushYunRequest : NSObject
+(void)lanch:(void (^)(NSError*))resultCallBack;
+(void)feedbackMessage:(NSString*)msgId;
+(void)registerWithPushYunChannel:(NSString *)channel result:(void (^)(NSError*))resultCallBack;
+(void)unRegisterWithPushYunChannel:(NSString *)channel;
@end
