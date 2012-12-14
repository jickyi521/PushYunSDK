package com.augmentum.pushyun.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.augmentum.pushyun.PushGlobals;
import com.augmentum.pushyun.util.StrUtils;

/**
 * TODO The title, message's color and size should be consistency with system. Currently defined by
 * customer, need to verify
 */
public class CustomerPushNotificationBuilder implements PushNotificationBuilder
{

    private int mCustomerLayout;
    private int mLayoutSubjectId;
    private int mLayoutMessageId;
    private int mLayoutIconId;
    private int mStatusBarIconDrawableId = PushGlobals.getAppInfo().icon;
    private Uri mSoundUri;

    /**
     * 
     * @param customerLayout The layout resource to use with notification layout
     * @param layoutSubjectId The layout resource to display title
     * @param layoutMessageId The layout resource to display content
     * @param layoutIconId The icon's id within customer layout
     * @param statusBarIconDrawableId The customer status icon want to display
     * @param soundUri The notification sound
     */
    public CustomerPushNotificationBuilder(int customerLayout, int layoutSubjectId, int layoutMessageId, int layoutIconId,
            int statusBarIconDrawableId, Uri soundUri)
    {
        mCustomerLayout = customerLayout;
        mLayoutSubjectId = layoutSubjectId;
        mLayoutMessageId = layoutMessageId;
        mLayoutIconId = layoutIconId;
        mStatusBarIconDrawableId = statusBarIconDrawableId;
        mSoundUri = soundUri;
    }

    @Override
    public Notification buildNotification(String notiificationId, String title, String content)
    {
        if (StrUtils.isEmpty(content)) return null;

        PreFroyoNotificationStyleDiscover.getInstance().discoverStyle();

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.setComponent(new ComponentName(PushGlobals.getPackageName(), PushGlobals.getLaunchActivityPathName()));
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.putExtra(PushNotificationBuilder.NOTIFICATION_ID, notiificationId);
        PendingIntent pengingIntent = PendingIntent.getActivity(PushGlobals.getAppContext(), 0, intent, 0);

        Notification customerNotification = new Notification(this.mStatusBarIconDrawableId, title, System.currentTimeMillis());
        RemoteViews customerRemoteView = new RemoteViews(PushGlobals.getPackageName(), mCustomerLayout);
        customerRemoteView.setTextViewText(mLayoutSubjectId, PushGlobals.getAppName());
        customerRemoteView.setTextViewText(mLayoutMessageId, content);
        customerRemoteView.setImageViewResource(mLayoutIconId, mStatusBarIconDrawableId);
        customerNotification.flags |= Notification.FLAG_AUTO_CANCEL;

        if (mSoundUri != null)
        {
            customerNotification.sound = mSoundUri;
        }
        else
        {
            customerNotification.defaults = Notification.DEFAULT_SOUND;
        }
        customerNotification.contentIntent = pengingIntent;

        // Some SAMSUNG devices status bar cant't show two lines with the size,
        // so need to verify it, maybe increase the height or decrease the font size

        customerRemoteView.setFloat(mLayoutSubjectId, "setTextSize", PreFroyoNotificationStyleDiscover.getInstance().getTitleSize());
        customerRemoteView.setTextColor(mLayoutSubjectId, PreFroyoNotificationStyleDiscover.getInstance().getTitleColor());

        customerRemoteView.setFloat(mLayoutMessageId, "setTextSize", PreFroyoNotificationStyleDiscover.getInstance().getTextSize());
        customerRemoteView.setTextColor(mLayoutMessageId, PreFroyoNotificationStyleDiscover.getInstance().getTextColor());

        customerNotification.contentView = customerRemoteView;

        return customerNotification;
    }

    /**
     * A class for discovering Android Notification styles on Pre-Froyo (2.3) devices
     */
    private static class PreFroyoNotificationStyleDiscover
    {
        private Integer mNotifyTextColor = null;
        private float mNotifyTextSize = 11;
        private Integer mNotifyTitleColor = null;
        private float mNotifyTitleSize = 12;
        private final String TEXT_SEARCH_TEXT = "SearchForText";
        private final String TEXT_SEARCH_TITLE = "SearchForTitle";

        private static PreFroyoNotificationStyleDiscover mPreFroyoNotificationStyleDiscover = null;
        private static Context mContext = PushGlobals.getAppContext();

        public static PreFroyoNotificationStyleDiscover getInstance()
        {
            if (mPreFroyoNotificationStyleDiscover == null)
            {
                mPreFroyoNotificationStyleDiscover = new PreFroyoNotificationStyleDiscover();
            }
            return mPreFroyoNotificationStyleDiscover;
        }

        public int getTextColor()
        {
            return mNotifyTextColor.intValue();
        }

        public float getTextSize()
        {
            return mNotifyTextSize;
        }

        public int getTitleColor()
        {
            return mNotifyTitleColor;
        }

        public float getTitleSize()
        {
            return mNotifyTitleSize;
        }

        private void discoverStyle()
        {
            // Already done
            if (null != mNotifyTextColor) { return; }

            try
            {
                Notification notify = new Notification();
                notify.setLatestEventInfo(mContext, TEXT_SEARCH_TITLE, TEXT_SEARCH_TEXT, null);
                LinearLayout group = new LinearLayout(mContext);
                ViewGroup event = (ViewGroup)notify.contentView.apply(mContext, group);
                recurseGroup(event);
                group.removeAllViews();
            }
            catch (Exception e)
            {
                // Default to something
                mNotifyTextColor = android.R.color.black;
                mNotifyTitleColor = android.R.color.black;
            }
        }

        private boolean recurseGroup(ViewGroup group)
        {
            final int count = group.getChildCount();

            for (int i = 0; i < count; ++i)
            {
                if (group.getChildAt(i) instanceof TextView)
                {
                    final TextView tv = (TextView)group.getChildAt(i);
                    final String text = tv.getText().toString();
                    if (text.startsWith("SearchFor"))
                    {
                        DisplayMetrics metrics = new DisplayMetrics();
                        WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
                        wm.getDefaultDisplay().getMetrics(metrics);

                        if (TEXT_SEARCH_TEXT == text)
                        {
                            mNotifyTextColor = tv.getTextColors().getDefaultColor();
                            mNotifyTextSize = tv.getTextSize();
                            mNotifyTextSize /= metrics.scaledDensity;
                        }
                        else
                        {
                            mNotifyTitleColor = tv.getTextColors().getDefaultColor();
                            mNotifyTitleSize = tv.getTextSize();
                            mNotifyTitleSize /= metrics.scaledDensity;
                        }

                        if (null != mNotifyTitleColor && mNotifyTextColor != null) { return true; }
                    }
                }
                else if (group.getChildAt(i) instanceof ViewGroup)
                {
                    if (recurseGroup((ViewGroup)group.getChildAt(i))) { return true; }
                }
            }
            return false;
        }
    }

}
