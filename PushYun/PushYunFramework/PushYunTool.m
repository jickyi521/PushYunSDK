//
//  PushYunTool.m
//  PushYun
//
//  Created by Alvin.Zeng on 12/11/12.
//  Copyright (c) 2012 Alvin.Zeng. All rights reserved.
//

#import "PushYunTool.h"

#define PUSH_YUN_APP_KEY_NAME @"PUSH_YUN_APP_KEY_NAME"
#define PUSH_YUN_TOKEN_KEY_NAME @"PUSH_YUN_TOKEN_KEY_NAME"

@implementation PushYunTool

static NSString *pushYunAppKey = @"";
static NSString *pushYunChannel = @"";
static NSString *pushYunDeviceToken = @"";
static PushYunDebugLevel debugLevel = PUSHYUN_DEBUG_ERRORS_ONLY;

+(void)setAppKey:(NSString *)appKey
{
    pushYunAppKey = appKey;
    
    NSUserDefaults *ud = [NSUserDefaults standardUserDefaults];
    [ud setObject:appKey forKey:PUSH_YUN_APP_KEY_NAME];
}

+(void)setChannel:(NSString *)channel
{
    pushYunChannel = channel;
}

+(void)setDeviceToken:(NSString *)token
{
    pushYunDeviceToken = token;
    
    NSUserDefaults *ud = [NSUserDefaults standardUserDefaults];
    [ud setObject:token forKey:PUSH_YUN_TOKEN_KEY_NAME];
}

+(NSString *)getAppVersion
{
    return [[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleVersion"];
}

+(NSString *)getDiveceName
{
    return [[UIDevice currentDevice] name];
}

+(NSString *)getUdid
{
    return [[UIDevice currentDevice] uniqueGlobalDeviceIdentifier];
}

+(NSString *)getAppKey
{
    if(nil == pushYunAppKey || [@"" isEqualToString:pushYunAppKey])
    {
        NSUserDefaults *ud = [NSUserDefaults standardUserDefaults];
        pushYunAppKey = [ud objectForKey:PUSH_YUN_APP_KEY_NAME];
    }
    
    return pushYunAppKey;
}

+(NSString *)getChannel
{
    return pushYunChannel;
}

+(NSString *)getToken
{
    if(nil == pushYunDeviceToken || [@"" isEqualToString:pushYunDeviceToken])
    {
        NSUserDefaults *ud = [NSUserDefaults standardUserDefaults];
        pushYunDeviceToken = [ud objectForKey:PUSH_YUN_TOKEN_KEY_NAME];
    }
    
    return pushYunDeviceToken;
}

+ (NSString *)generateSignCode:(NSDictionary*)params
{
    return @"";
}

#pragma mark Log control

+(void)setDebugLevel:(PushYunDebugLevel)aDebugLevel
{
    debugLevel = aDebugLevel;
}

+ (void)log:(NSString *)message debugLevel:(PushYunDebugLevel)level
{
    if(debugLevel != PUSHYUN_DEBUG_NONE && level <= debugLevel)
    {
        NSLog(@"%@", message);
    }
}

NSString *composeString(NSString *formatString, ...)
{
    NSString *reason = @"";
    if (formatString) {
        va_list vl;
        va_start(vl, formatString);
        reason = [[NSString alloc] initWithFormat:formatString arguments:vl];
        va_end(vl);
    }
    return reason;
}
@end
