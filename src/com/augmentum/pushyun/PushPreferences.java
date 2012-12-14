package com.augmentum.pushyun;

import android.content.Context;
import android.content.SharedPreferences;

public class PushPreferences
{
    // TODO Extra project's preferences here
    private static final String PUSHYUN_PREFERENCES = "com.augmentum.pushyun";

    private static PushPreferences mPushPreferences = new PushPreferences();

    private SharedPreferences mSharedPreferences = getPushyunReferences(PushGlobals.getAppContext());
    private SharedPreferences.Editor mEditor = mSharedPreferences.edit();

    private PushPreferences()
    {
        // Empty singleton mode
    }

    public static PushPreferences getInstance()
    {
        if (mPushPreferences == null)
        {
            mPushPreferences = new PushPreferences();
        }
        return mPushPreferences;
    }

    public int getIntValue(String key, int defaultValue)
    {
        return mSharedPreferences.getInt(key, defaultValue);
    }

    public void setIntValue(String key, int value)
    {
        mEditor.putInt(key, value);
        commit();
    }

    public boolean getBooleanValue(String key, boolean defaultValue)
    {
        return mSharedPreferences.getBoolean(key, defaultValue);
    }

    public void setBooleanValue(String key, boolean value)
    {
        mEditor.putBoolean(key, value);
        commit();
    }

    private void commit()
    {
        mEditor.commit();
    }

    private SharedPreferences getPushyunReferences(Context context)
    {
        return context.getSharedPreferences(PUSHYUN_PREFERENCES, 0);
    }
}
