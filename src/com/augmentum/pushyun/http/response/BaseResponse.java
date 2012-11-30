package com.augmentum.pushyun.http.response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import android.util.Log;

public class BaseResponse
{
    HttpResponse mRresponse;
    String mBody;

    public BaseResponse(HttpResponse paramHttpResponse)
    {
        this.mRresponse = paramHttpResponse;
    }

    public int status()
    {
        StatusLine localStatusLine = this.mRresponse.getStatusLine();
        if (localStatusLine != null) return this.mRresponse.getStatusLine().getStatusCode();
        return -1;
    }
    
    public boolean isStatusOk()
    {
        return status() == 200;
    }

    public String reason()
    {
        StatusLine localStatusLine = this.mRresponse.getStatusLine();
        if (localStatusLine != null) return this.mRresponse.getStatusLine().getReasonPhrase();
        return "";
    }

    public long length()
    {
        HttpEntity localHttpEntity = this.mRresponse.getEntity();
        if (localHttpEntity != null) return this.mRresponse.getEntity().getContentLength();
        return 0L;
    }

    public String contentType()
    {
        Header localHeader = this.mRresponse.getFirstHeader("Content-Type");
        if (localHeader != null) return localHeader.getValue();
        return "";
    }

    public JSONObject getJSONData()
    {
        JSONObject jsonData = null;
        try
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(rawBody()));
            StringBuffer buffer = new StringBuffer("");
            buffer = new StringBuffer("");
            String line = "";
            String NL = System.getProperty("line.separator");
            while ((line = in.readLine()) != null)
            {
                buffer.append(line + NL);
                if (!in.ready())
                {
                    break;
                }
            }
            jsonData = new JSONObject(buffer.toString());

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return jsonData;
    }

    public Header getFirstHeader(String paramString)
    {
        Header localHeader = this.mRresponse.getFirstHeader(paramString);
        return localHeader;
    }

    public InputStream rawBody() throws IllegalStateException, IOException
    {
        HttpEntity localHttpEntity = this.mRresponse.getEntity();
        if (localHttpEntity != null) return this.mRresponse.getEntity().getContent();
        return null;
    }

    public String body()
    {
        if (this.mBody == null)
        {
            this.mBody = "";
            if (this.mRresponse.getEntity() != null) try
            {
                this.mBody = EntityUtils.toString(this.mRresponse.getEntity());
            }
            catch (IOException localIOException)
            {
                Log.e("Response", "Error fetching HTTP entity: IO Exception");
            }
        }
        return this.mBody;
    }
}
