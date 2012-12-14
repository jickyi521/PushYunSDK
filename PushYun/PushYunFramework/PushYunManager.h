//
//  PushYunManager.h
//  PushYun
//
//  Created by Alvin.Zeng on 12/10/12.
//  Copyright (c) 2012 Alvin.Zeng. All rights reserved.
//

#import <Foundation/Foundation.h>

typedef enum  {
    PUSHYUN_DEBUG_NONE = 0,
    PUSHYUN_DEBUG_ERRORS_ONLY = 1,
    PUSHYUN_DEBUG_ACTIVITY = 2,
    PUSHYUN_DEBUG_VERBOSE = 3
} PushYunDebugLevel;

// IMPORTANT: To use the Push Yun service, you are required to have a PushYun API key. You can
// obtain this by setting up an account and adding an app at http://pushyun.com

@protocol PushYunManagerDelegate <NSObject>
- (void)readyForRegistration;
- (void)registrationSucceeded;
- (void)registrationFailedWithError:(NSError *)error;
@end

@interface PushYunManager : NSObject

// Set a delegate to get messages about push status, or to make calls that need to call back.
@property (nonatomic, assign) id <PushYunManagerDelegate> delegate;

//
//Init
//
//Methods to initialize push yun app key.
- (void) initializePushYunAppKey:(NSString *)appKey delegate:(id <PushYunManagerDelegate>)delegate;

//
// Setup
//
// Methods to be integrated into your app lifecycle to help us configure your app for Push Yun
- (void) didFinishLaunchingWithOptions:(NSDictionary *)launchOptions;
- (void) didReceiveRemoteNotification:(NSDictionary *)userInfo;
- (void) didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken;
- (void) didFailToRegisterForRemoteNotificationsWithError:(NSError *)error;

//
//
// Simple (Broadcast Push) Registration
//
// Register a device with Push Yun so you can broadcast to a user, without them opting into specific categories.
- (void) registerWithPushYunChannel:(NSString *)channel;

// Delete a device from Push Yun
- (void) unregisterFromPushYunChannel:(NSString *)channel;

// See the PushYunDebugLevel enum for valid debug levels.
- (void)setDebugLevel:(PushYunDebugLevel)debugLevel;

//
// Singleton instance
//
+ (PushYunManager *) sharedInstance;

@end
