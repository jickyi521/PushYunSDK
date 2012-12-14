//
//  PushYunManager.m
//  PushYun
//
//  Created by Alvin.Zeng on 12/10/12.
//  Copyright (c) 2012 Alvin.Zeng. All rights reserved.
//

#import "PushYunManager.h"
#import "PushYunRequest.h"
#import "PushYunTool.h"

@interface PushYunManager (Private)
-(void)handleNotification:(NSDictionary *)userInfo;
@end

@implementation PushYunManager
@synthesize delegate = _delegate;

#pragma mark Setup
- (void) initializePushYunAppKey:(NSString *)appKey delegate:(id <PushYunManagerDelegate>)delegate
{
    self.delegate = delegate;
    
    if(appKey == nil || [@"" isEqualToString:appKey])
    {
        DNSLog(PUSHYUN_DEBUG_ERRORS_ONLY, @"App Key cannot be empty!");
    }

    [PushYunTool setAppKey:appKey];
}

-(void)didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    if(nil != launchOptions && [launchOptions objectForKey:UIApplicationLaunchOptionsRemoteNotificationKey])
    {
        //if we're here, apps was launched due to Remote Notification
        NSDictionary *userInfo = [launchOptions objectForKey:UIApplicationLaunchOptionsRemoteNotificationKey];
        [self handleNotification:userInfo];
    }
}

- (void) didReceiveRemoteNotification:(NSDictionary *)userInfo
{
    //Received Remote Notificatation when App has lanuched.
    
    if([UIApplication sharedApplication].applicationState != UIApplicationStateActive)
    {
        //if we're here, apps was in foreground due to Remote Notification
        [self handleNotification:userInfo];
    }
    
}
- (void) didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken
{
    const char *devTokenBytes = [deviceToken bytes];
    
    int len = [deviceToken length];
    char temps[1024];
    temps[0] = '\0';
    for ( int i=0; i<len; i++ )
    {
        char temps2[100];
        sprintf(temps2, "%02x", (unsigned char) devTokenBytes[i] );
        strcat( temps, temps2 );
    }
    
    NSString *strToken = [NSString stringWithCString:temps encoding:NSUTF8StringEncoding];
    
    [PushYunTool setDeviceToken:strToken];
    
    DNSLog(PUSHYUN_DEBUG_VERBOSE, @"Get Device token successfully, Token %@", strToken);
    
    //Send token to SaaS
    void (^resultCallBack) (NSError*) = ^(NSError *error)
    {
        if(error)
        {
            DNSLog(PUSHYUN_DEBUG_ERRORS_ONLY, @"Failed to add device to Push Yun, %@", error);
        }
        else
        {
            if(_delegate && [_delegate respondsToSelector:@selector(readyForRegistration)])
            {
                [_delegate readyForRegistration];
            }
            else
            {
                DNSLog(PUSHYUN_DEBUG_VERBOSE, @"Delegate is nil or delegate not responds method readyForRegistration");
            }
        }
    };
    
    [PushYunRequest lanch:resultCallBack];
}
- (void) didFailToRegisterForRemoteNotificationsWithError:(NSError *)error
{
    DNSLog(PUSHYUN_DEBUG_ERRORS_ONLY, @"Failed to get device token, Error %@", error);
    
    if(_delegate && [_delegate respondsToSelector:@selector(registrationFailedWithError:)])
    {
        [_delegate registrationFailedWithError:error];
    }
    else
    {
        DNSLog(PUSHYUN_DEBUG_VERBOSE, @"Delegate is nil or delegate not responds method registrationFailedWithError");
    }
}

#pragma mark Private mothed
-(void)handleNotification:(NSDictionary *)userInfo
{
    NSDictionary *payload = [userInfo objectForKey:@"aps"];
    NSString *alertMessageId = [payload objectForKey:@"nid"];
    
    DNSLog(PUSHYUN_DEBUG_VERBOSE, @"Notification message id %@", alertMessageId);
    
    if(nil == alertMessageId || [@"" isEqualToString:alertMessageId])
    {
        DNSLog(PUSHYUN_DEBUG_ACTIVITY, @"Received Notification, but message id is nil, maybe this notification isn't from Push Yun or SaaS error.");
    }
    else //Feedback to SaaS
    {
        [PushYunRequest feedbackMessage:alertMessageId];
    }
}

#pragma mark Simple (Broadcast Push) Registration
-(void)registerWithPushYunChannel:(NSString *)channel
{
    void (^resultCallBack) (NSError*) = ^(NSError *error)
    {
        if(error)
        {
            DNSLog(PUSHYUN_DEBUG_ERRORS_ONLY, @"Failed to register device to channel %@, Error %@", channel, error);
            
            if(_delegate && [_delegate respondsToSelector:@selector(registrationFailedWithError:)])
            {
                [_delegate registrationFailedWithError:error];
            }
            else
            {
                DNSLog(PUSHYUN_DEBUG_ACTIVITY, @"Delegate is nil or delegate not responds method registrationFailedWithError");
            }
        }
        else
        {
            
            DNSLog(PUSHYUN_DEBUG_VERBOSE, @"Register device to channel %@ successfully.", channel);
            
            if(_delegate && [_delegate respondsToSelector:@selector(registrationSucceeded)])
            {
                [_delegate registrationSucceeded];
            }
            else
            {
                DNSLog(PUSHYUN_DEBUG_ACTIVITY, @"Delegate is nil or delegate not responds method registrationSucceeded");
            }
        }
    };

    if(nil != [PushYunTool getToken] && ![@"" isEqualToString:[PushYunTool getToken]])
    {
        [PushYunRequest registerWithPushYunChannel:channel result:resultCallBack];
    }
    else
    {
        DNSLog(PUSHYUN_DEBUG_VERBOSE, @"Haven't got token, please call this method at readyForRegistration.");
    }
}

-(void)unregisterFromPushYunChannel:(NSString *)channel
{
    [PushYunRequest unRegisterWithPushYunChannel:channel];
}

-(void)setDebugLevel:(PushYunDebugLevel)debugLevel
{
    [PushYunTool setDebugLevel:debugLevel];
}

#pragma mark Singleton instance
static PushYunManager *sharedPushYunManager = nil;

+ (PushYunManager*)sharedInstance
{
    @synchronized(self)
    {
        if (sharedPushYunManager == nil)
        {
            sharedPushYunManager = [[super allocWithZone:NULL] init];
        }
    
        return sharedPushYunManager;
    }
}

+ (id)allocWithZone:(NSZone *)zone
{
    return [self sharedInstance];
}

- (id)copyWithZone:(NSZone *)zone
{
    return self;
}

@end
