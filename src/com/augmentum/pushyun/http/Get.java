package com.augmentum.pushyun.http;

import java.io.IOException;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;

import com.augmentum.pushyun.http.response.BaseResponse;

public class Get extends HttpGet
{
    protected DefaultHttpClient mHttpclient = null;

    public Get(String endPointURL, List<BasicNameValuePair> nameValuePairs)
    {
        super(generateGetURL(endPointURL, nameValuePairs));
        BasicHttpParams localBasicHttpParams = new BasicHttpParams();
        mHttpclient = new DefaultHttpClient(localBasicHttpParams);
        HttpConnectionParams.setConnectionTimeout(localBasicHttpParams, 10000);
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

    public static String generateGetURL(String endPointURL, List<BasicNameValuePair> nameValuePairs)
    {
        String combinedParams = "";
        if (!nameValuePairs.isEmpty())
        {
            combinedParams += "?";
            for (NameValuePair p : nameValuePairs)
            {
                String paramString = p.getName() + "=" + p.getValue();
                if (combinedParams.length() > 1)
                {
                    combinedParams += "&" + paramString;
                }
                else
                {
                    combinedParams += paramString;
                }
            }
        }

        return endPointURL + combinedParams;

    }
}
