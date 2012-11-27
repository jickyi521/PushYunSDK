package com.augmentum.pushyun.http.request;

import java.io.IOException;
import java.net.URI;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;

import android.util.Log;

import com.augmentum.pushyun.http.response.BaseResponse;

public class BaseRequest extends HttpEntityEnclosingRequestBase
{
    String method;
    DefaultHttpClient httpClient;

    public BaseRequest(String paramString1, String paramString2)
    {
        
        this.method = paramString1;
        setURI(URI.create(paramString2));
        this.params = new BasicHttpParams();
        this.httpClient = new DefaultHttpClient(this.params);
        setSocketBufferSize(16384);
        setTimeout(60000);
        Log.v("Request", "Set Timeout: " + HttpConnectionParams.getConnectionTimeout(this.httpClient.getParams()));
        Log.v("Request", "Set Socket Buffer Size: " + HttpConnectionParams.getSocketBufferSize(this.httpClient.getParams()));
    }

    public void setTimeout(int paramInt)
    {
        HttpConnectionParams.setConnectionTimeout(this.params, paramInt);
    }

    public void setSocketBufferSize(int paramInt)
    {
        HttpConnectionParams.setSocketBufferSize(this.params, paramInt);
    }

    public void setAuth(String paramString1, String paramString2)
    {
        UsernamePasswordCredentials localUsernamePasswordCredentials = new UsernamePasswordCredentials(paramString1, paramString2);
        BasicCredentialsProvider localBasicCredentialsProvider = new BasicCredentialsProvider();
        localBasicCredentialsProvider.setCredentials(AuthScope.ANY, localUsernamePasswordCredentials);
        this.httpClient.setCredentialsProvider(localBasicCredentialsProvider);
    }

    public String getMethod()
    {
        return this.method;
    }

    public BaseResponse execute()
    {
        BaseResponse localResponse = null;
        try
        {
            localResponse = new BaseResponse(this.httpClient.execute(this));
        }
        catch (IOException localIOException)
        {
            Log.v("Request", "IOException when executing request. Do you have permission to access the internet?");
            Log.v("Request", localIOException.getMessage());
        }
        return localResponse;
    }

}
