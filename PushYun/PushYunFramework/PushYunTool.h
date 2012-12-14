//
//  PushYunTool.h
//  PushYun
//
//  Created by Alvin.Zeng on 12/11/12.
//  Copyright (c) 2012 Alvin.Zeng. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "PushYunManager.h"

extern NSString *composeString(NSString *, ...);

#define DNSLog(PushYunDebugLevel, description, ...) \
[PushYunTool log:[NSString stringWithFormat:(@"%s [Line %d]  %@"), __PRETTY_FUNCTION__, __LINE__, composeString(description, ##__VA_ARGS__)]  debugLevel:PushYunDebugLevel]

@interface PushYunTool : NSObject
+ (NSString *)getAppKey;
+ (NSString *)getToken;
+ (NSString *)getAppVersion;
+ (NSString *)getDiveceName;
+ (NSString *)getChannel;
+ (NSString *)getUdid;
+ (NSString *)generateSignCode:(NSDictionary*)params;

+ (void)setAppKey:(NSString *)appKey;
+ (void)setChannel:(NSString *)channel;
+ (void)setDeviceToken:(NSString *)token;

+ (void)setDebugLevel:(PushYunDebugLevel)aDebugLevel;
+ (void)log:(NSString *)message debugLevel:(PushYunDebugLevel)level;
@end
