//
//  PushYunAppDelegate.h
//  PushYun
//
//  Created by Alvin.Zeng on 12/10/12.
//  Copyright (c) 2012 Alvin.Zeng. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "PushYunManager.h"

@class PushYunViewController;

@interface PushYunAppDelegate : UIResponder <UIApplicationDelegate, PushYunManagerDelegate>

@property (strong, nonatomic) UIWindow *window;

@property (strong, nonatomic) PushYunViewController *viewController;

@end
