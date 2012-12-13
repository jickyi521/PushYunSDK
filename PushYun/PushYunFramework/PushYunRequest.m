//
//  PushYunRequest.m
//  PushYun
//
//  Created by Alvin.Zeng on 12/11/12.
//  Copyright (c) 2012 Alvin.Zeng. All rights reserved.
//

#import "PushYunRequest.h"

@interface PushYunRequest ()
+(NSDictionary*)generateRegisterParams:(NSString *)channel;
+(void)customError:(NSError **)error resonse:(id)response urlResponse:(NSHTTPURLResponse *)urlResponse;
+(void)sendPostRequest:(NSString *)url params:(NSDictionary*)params result:(void (^)(NSError*))resultCallBack;
@end

@implementation PushYunRequest

+(void)lanch:(void (^)(NSError*))resultCallBack
{
    NSMutableDictionary *params = [[NSMutableDictionary alloc] init];
    
    [params setObject:API_VERSION forKey:API_VERSION_NAME];
    [params setObject:PLATFORM forKey:PLATFORM_NAME];
    [params setObject:API_KEY forKey:API_KEY_NAME];
    [params setObject:[PushYunTool getAppKey] forKey:APP_KEY_NAME];
    [params setObject:[PushYunTool getToken] forKey:TOKEN_NAME];
    [params setObject:[PushYunTool getAppVersion] forKey:APP_VERSION_NAME];
    [params setObject:[PushYunTool getDiveceName] forKey:DEVICE_NAME_NAME];
    [params setObject:[PushYunTool getChannel] forKey:CHANNEL_NAME];
    [params setObject:[PushYunTool getUdid] forKey:UDID_NAME];
    
    NSString *signCode = [PushYunTool generateSignCode:params];
    [params setObject:signCode forKey:SIGN_NAME];
    
    [self sendPostRequest:LANUNCNED_API_URL params:params result:resultCallBack];
    
}

+(void)feedbackMessage:(NSString *)msgId
{
    NSMutableDictionary *params = [[NSMutableDictionary alloc] init];
    
    [params setObject:API_VERSION forKey:API_VERSION_NAME];
    [params setObject:PLATFORM forKey:PLATFORM_NAME];
    [params setObject:API_KEY forKey:API_KEY_NAME];
    [params setObject:[PushYunTool getAppKey] forKey:APP_KEY_NAME];
    [params setObject:msgId forKey:NID_NAME];
    
    NSString *signCode = [PushYunTool generateSignCode:params];
    [params setObject:signCode forKey:SIGN_NAME];
    
    [self sendPostRequest:FEEDBACK_API_URL params:params result:nil];
}

+(void)registerWithPushYunChannel:(NSString *)channel result:(void (^)(NSError*))resultCallBack
{
    [self sendPostRequest:REGISTER_API_URL params:[self generateRegisterParams:channel] result:resultCallBack];
}

+(void)unRegisterWithPushYunChannel:(NSString *)channel
{
    [self sendPostRequest:UNREGISTER_API_URL params:[self generateRegisterParams:channel] result:nil];
}


#pragma mark Private motheds
+(void)sendPostRequest:(NSString *)url params:(NSDictionary *)params result:(void (^)(NSError *))resultCallBack
{
    DNSLog(PUSHYUN_DEBUG_VERBOSE, @"[POST] %@", url);
    
    [SVHTTPRequest POST:url parameters:params completion:^(id response, NSHTTPURLResponse *urlResponse, NSError *error)
     {
         DNSLog(PUSHYUN_DEBUG_ACTIVITY, @"URL %@ StatusCode %d", url, urlResponse.statusCode);
         
         if(error == nil)
         {
             [self customError:&error resonse:response urlResponse:urlResponse];
         }
         else
         {
             DNSLog(PUSHYUN_DEBUG_ERRORS_ONLY, @"Request URL %@, %@", url, error);
         }
         
         if(resultCallBack)
         {
             resultCallBack(error);
         }
         
     }];
}

+(void)customError:(NSError *__autoreleasing *)error resonse:(id)response urlResponse:(NSHTTPURLResponse *)urlResponse
{
    if(response)
    {
        id jsonObject = [NSJSONSerialization JSONObjectWithData:response options:NSJSONReadingAllowFragments error:nil];
        
        NSString * ack = [jsonObject valueForKey:@"ack"];
        NSString *message = [jsonObject valueForKey:@"message"];
        
        //Recived response from SaaS
        if(nil != ack && ![@"" isEqualToString:ack])
        {
            //SaaS Error
            if([ack integerValue] != 0)
            {
                NSDictionary *userInfo = [NSDictionary dictionaryWithObject:message == nil ? @"Bad Response." : message                                                                      forKey:NSLocalizedDescriptionKey];
                *error = [NSError errorWithDomain:@"com.pushyun.www" code:[ack integerValue] userInfo:userInfo];
            }
            
            return;
        }
    }

    //Default Error info
    NSDictionary *userInfo = [NSDictionary dictionaryWithObject:@"Bad Response."                                                                      forKey:NSLocalizedDescriptionKey];
    *error = [NSError errorWithDomain:@"com.pushyun.www" code:urlResponse.statusCode userInfo:userInfo];
    
}

+(NSDictionary *)generateRegisterParams:(NSString *)channel
{
    NSMutableDictionary *params = [[NSMutableDictionary alloc] init];
    
    [params setObject:API_VERSION forKey:API_VERSION_NAME];
    [params setObject:PLATFORM forKey:PLATFORM_NAME];
    [params setObject:API_KEY forKey:API_KEY_NAME];
    [params setObject:[PushYunTool getAppKey] forKey:APP_KEY_NAME];
    [params setObject:[PushYunTool getToken] forKey:TOKEN_NAME];
    [params setObject:channel forKey:CHANNEL_NAME];
    [params setObject:[PushYunTool getAppVersion] forKey:APP_VERSION_NAME];
    [params setObject:[PushYunTool getUdid] forKey:UDID_NAME];
    
    NSString *signCode = [PushYunTool generateSignCode:params];
    [params setObject:signCode forKey:SIGN_NAME];
    
    return params;
}

+(BOOL)validateParams:(NSDictionary *)params
{
    return YES;
}

@end
