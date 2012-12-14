package com.augmentum.pushyun.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;

import com.augmentum.pushyun.common.Logger;
import com.augmentum.pushyun.http.response.BaseResponse;

public class Post extends HttpPost
{
    protected DefaultHttpClient mHttpclient = null;

    public Post(String endPointURL, List<BasicNameValuePair> nameValuePairs)
    {
        super(endPointURL);
        BasicHttpParams localBasicHttpParams = new BasicHttpParams();
        mHttpclient = new DefaultHttpClient(localBasicHttpParams);
        HttpConnectionParams.setConnectionTimeout(localBasicHttpParams, 10000);
        UrlEncodedFormEntity localUrlEncodedFormEntity = null;
        try
        {
            localUrlEncodedFormEntity = new UrlEncodedFormEntity(nameValuePairs, "UTF-8");
            setEntity(localUrlEncodedFormEntity);
        }
        catch (UnsupportedEncodingException localUnsupportedEncodingException)
        {
            Logger.error(Logger.HTTP_LOG_TAG, "UTF-8 Unsupported.", localUnsupportedEncodingException);
        }
    }

    public BaseResponse execute()
    {
        try
        {
            return new BaseResponse(this.mHttpclient.execute(this));
        }
        catch (IOException e)
        {
            return null;
        }
    }

    public void release()
    {
        mHttpclient.getConnectionManager().shutdown();
    }
}