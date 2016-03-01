package com.perasia.gameplay;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadManager {
    private static final String TAG = DownloadManager.class.getSimpleName();

    private static final int THREAD_POOL_SIZE = 4;

    public interface DownloadCallBack {
        void onStart();

        void onLoading(long total, long current, boolean isUploading);

        void onSuccess(ResponseInfo<File> responseInfo);

        void onFailure(HttpException error, String msg);
    }

    private Context mContext;

    private static DownloadManager mDownloadManager;

    private DownloadCallBack mCallBack;

    private String mFilePath;

    private static List<String> mDownloadLists = new ArrayList<>();

    private boolean mShowNotify = false;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;

    private DownloadManager(Context context) {
        mContext = context;
    }

    public static DownloadManager getInstance(Context context) {
        if (mDownloadManager == null) {
            mDownloadManager = new DownloadManager(context);
        }

        return mDownloadManager;
    }

    public void execute(String downloadUrl, String filePath, boolean showNotify, DownloadCallBack callBack) {
        if (mDownloadLists.contains(filePath)) {
            Log.e(TAG, "downloading");
            return;
        }

        mFilePath = filePath;
        mDownloadLists.add(filePath);

        mShowNotify = showNotify;

        this.mCallBack = callBack;

        HttpUtils http = new HttpUtils();
        http.configRequestThreadPoolSize(THREAD_POOL_SIZE);
        http.download(downloadUrl, filePath, false, false, new MyCallBack());

        if (mShowNotify) {
            if (mContext != null) {
                showNotify();
            }
        }
    }

    private void showNotify() {
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        mBuilder = new NotificationCompat.Builder(mContext)
                .setContentTitle(mContext.getString(R.string.download))
                .setPriority(Notification.PRIORITY_MAX)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(mContext.getString(R.string.download_loading))
                .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_launcher));

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            Intent intent = new Intent();
            PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
            mBuilder.setContentIntent(contentIntent);
        }

        mNotificationManager.notify(0, mBuilder.build());
    }


    private class MyCallBack extends RequestCallBack<File> {
        @Override
        public void onStart() {
            super.onStart();
            if (mCallBack != null) {
                mCallBack.onStart();
            }
        }

        @Override
        public void onLoading(long total, long current, boolean isUploading) {
            super.onLoading(total, current, isUploading);
            if (mShowNotify) {
                mBuilder.setProgress((int) total, (int) current, false);
                try {
                    mBuilder.setContentText(CommonUtil.formatSize(current) + "/" +
                            CommonUtil.formatSize(total) +

                            " (" + current * 100 / total + "%)");
                } catch (ArithmeticException e) {
                    e.printStackTrace();
                }

                mNotificationManager.notify(0, mBuilder.build());
            }

            if (mCallBack != null) {
                mCallBack.onLoading(total, current, isUploading);
            }
        }

        @Override
        public void onSuccess(ResponseInfo<File> responseInfo) {
            mDownloadLists.remove(mFilePath);

            mNotificationManager.cancel(0);

            if (mCallBack != null) {
                mCallBack.onSuccess(responseInfo);
            }
        }

        @Override
        public void onFailure(HttpException error, String msg) {
            mDownloadLists.remove(mFilePath);

            mNotificationManager.cancel(0);

            if (mCallBack != null) {
                mCallBack.onFailure(error, msg);
            }
        }
    }

}
