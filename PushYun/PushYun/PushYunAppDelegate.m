//
//  PushYunAppDelegate.m
//  PushYun
//
//  Created by Alvin.Zeng on 12/10/12.
//  Copyright (c) 2012 Alvin.Zeng. All rights reserved.
//

#import "PushYunAppDelegate.h"

#import "PushYunViewController.h"


@implementation PushYunAppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    self.window = [[UIWindow alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
    // Override point for customization after application launch.
    self.viewController = [[PushYunViewController alloc] initWithNibName:@"PushYunViewController" bundle:nil];
    self.window.rootViewController = self.viewController;
    [self.window makeKeyAndVisible];
    
    [[PushYunManager sharedInstance] initializePushYunAppKey:@"6mh4dt2p3nbwuzl6e90g68ed" delegate:self];
    [[PushYunManager sharedInstance] didFinishLaunchingWithOptions:launchOptions];
    [[PushYunManager sharedInstance] setDebugLevel:PUSHYUN_DEBUG_VERBOSE];
    
    [[UIApplication sharedApplication] registerForRemoteNotificationTypes:UIRemoteNotificationTypeAlert|UIRemoteNotificationTypeBadge|UIRemoteNotificationTypeSound|UIRemoteNotificationTypeNewsstandContentAvailability];
    
    return YES;
}

- (void)applicationWillResignActive:(UIApplication *)application
{
    // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
    // Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
}

- (void)applicationDidEnterBackground:(UIApplication *)application
{
    // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later. 
    // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
}

- (void)applicationWillEnterForeground:(UIApplication *)application
{
    // Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
}

- (void)applicationDidBecomeActive:(UIApplication *)application
{
    // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
}

- (void)applicationWillTerminate:(UIApplication *)application
{
    // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
}
#pragma mark APNS

- (void)application:(UIApplication *)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken
{
    [[PushYunManager sharedInstance] didRegisterForRemoteNotificationsWithDeviceToken:deviceToken];
}

- (void)application:(UIApplication *)application didFailToRegisterForRemoteNotificationsWithError:(NSError *)error
{
    [[PushYunManager sharedInstance] didFailToRegisterForRemoteNotificationsWithError:error];
}

- (void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo
{
    [[PushYunManager sharedInstance] didReceiveRemoteNotification:userInfo];
    
    // Present the push notification via an AlertView if received when the app is running
    NSDictionary *payload = [userInfo objectForKey:@"aps"];
    NSString *alertMessage = [payload objectForKey:@"alert"];
    
    UIAlertView *alertView = [[UIAlertView alloc] initWithTitle:nil message:alertMessage delegate:self cancelButtonTitle:@"OK" otherButtonTitles:nil];
    [alertView show];
}


#pragma mark Push IO

- (void) readyForRegistration
{
    NSLog(@"Push Yun is ready for registration");
    
    // This will register for broadcast. Alternatively, register for categories to segment.
    [[PushYunManager sharedInstance] registerWithPushYunChannel:@"App"];
}

- (void)registrationSucceeded
{
    NSLog(@"Push Yun registration succeeded");
}

- (void)registrationFailedWithError:(NSError *)error
{
    NSLog(@"Push Yun registration failed %@", error);
}

@end
