package com.perasia.gameplay.download;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.lidroid.xutils.DbUtils;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.db.converter.ColumnConverter;
import com.lidroid.xutils.db.converter.ColumnConverterFactory;
import com.lidroid.xutils.db.sqlite.ColumnDbType;
import com.lidroid.xutils.db.sqlite.Selector;
import com.lidroid.xutils.exception.DbException;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.HttpHandler;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.util.LogUtils;
import com.perasia.gameplay.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JiDownloadManager {
    private static final String TAG = "DownloadManager";

    private class JiNotifyCompat {
        int id;
        NotificationCompat.Builder builder;
    }

    private static int sNotifyId = 100;

    NotificationManager mNotificationManager;

    private Map<String, JiNotifyCompat> mNotifyCompatMap = new HashMap<>();

    private List<JiDownloadInfo> mDownloadInfoList;

    private int maxDownloadThread = 4;
    private boolean mShowNotify = true;

    private Context mContext;
    private DbUtils db;

    private String mDownloadurl;

    public JiDownloadManager(Context appContext) {
        ColumnConverterFactory.registerColumnConverter(HttpHandler.State.class, new HttpHandlerStateConverter());
        mContext = appContext;
        db = DbUtils.create(mContext);
        try {
            mDownloadInfoList = db.findAll(Selector.from(JiDownloadInfo.class));
        } catch (DbException e) {
            LogUtils.e(e.getMessage(), e);
        }
        if (mDownloadInfoList == null) {
            mDownloadInfoList = new ArrayList<JiDownloadInfo>();
        }
    }

    public int getDownloadInfoListCount() {
        return mDownloadInfoList.size();
    }

    public List<JiDownloadInfo> getmDownloadInfoList() {

        return mDownloadInfoList;
    }

    public JiDownloadInfo getDownloadInfo(int index) {
        return mDownloadInfoList.get(index);
    }

    public JiDownloadInfo getCurrentDownloadInfo(int adid) {
        JiDownloadInfo downloadInfo = null;

        for (int i = 0; i < mDownloadInfoList.size(); i++) {
            downloadInfo = mDownloadInfoList.get(i);
            if (downloadInfo.getAdCode() == adid) {
                return downloadInfo;
            }
        }
        return null;
    }

    public void addNewDownload(String url, String fileName, String target, int adid,
                               boolean autoResume, boolean autoRename, boolean showNotify,
                               final RequestCallBack<File> callback) throws DbException {

        mDownloadurl = url;
        mShowNotify = showNotify;

        final JiDownloadInfo downloadInfo = new JiDownloadInfo();
        downloadInfo.setDownloadUrl(url);
        downloadInfo.setAutoRename(autoRename);
        downloadInfo.setAutoResume(autoResume);
        downloadInfo.setFileName(fileName);
        downloadInfo.setFileSavePath(target);
        downloadInfo.setAdCode(adid);

        HttpUtils http = new HttpUtils();
        http.configRequestThreadPoolSize(maxDownloadThread);

        HttpHandler<File> handler = http.download(url, target, autoResume, autoRename, new ManagerCallBack(downloadInfo, callback));

        downloadInfo.setHandler(handler);
        downloadInfo.setState(handler.getState());
        mDownloadInfoList.add(downloadInfo);
        db.saveBindingId(downloadInfo);

        if (mShowNotify) {
            JiNotifyCompat notifyCompat = mNotifyCompatMap.get(mDownloadurl);

            if (notifyCompat == null || notifyCompat.builder == null) {
                mNotifyCompatMap.remove(mDownloadurl);

                notifyCompat = new JiNotifyCompat();
                notifyCompat.id = getNotifyId();
                notifyCompat.builder = new NotificationCompat.Builder(mContext)
                        .setWhen(System.currentTimeMillis())
                        .setContentTitle(fileName)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setDefaults(Notification.DEFAULT_SOUND)
                        .setOnlyAlertOnce(true)
                        .setAutoCancel(true)
//                    .setOngoing(true)
//                    .setProgress(fileSize, complete, false)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_launcher));

                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                    Intent intent = new Intent();
                    PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
                    notifyCompat.builder.setContentIntent(contentIntent);
                }

                mNotifyCompatMap.put(mDownloadurl, notifyCompat);
            }
            mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

            mNotificationManager.notify(notifyCompat.id, notifyCompat.builder.build());
        }
    }

    public void resumeDownload(int index, final RequestCallBack<File> callback) throws DbException {
        final JiDownloadInfo downloadInfo = mDownloadInfoList.get(index);
        resumeDownload(downloadInfo, callback);
    }

    public void resumeDownload(JiDownloadInfo downloadInfo, final RequestCallBack<File> callback) throws DbException {
        HttpUtils http = new HttpUtils();
        http.configRequestThreadPoolSize(maxDownloadThread);
        HttpHandler<File> handler = http.download(
                downloadInfo.getDownloadUrl(),
                downloadInfo.getFileSavePath(),
                downloadInfo.isAutoResume(),
                downloadInfo.isAutoRename(),
                new ManagerCallBack(downloadInfo, callback));
        downloadInfo.setHandler(handler);
        downloadInfo.setState(handler.getState());
        db.saveOrUpdate(downloadInfo);
    }

    public void removeDownload(int index) throws DbException {
        JiDownloadInfo downloadInfo = mDownloadInfoList.get(index);
        removeDownload(downloadInfo);
    }

    public void removeDownload(JiDownloadInfo downloadInfo) throws DbException {
        HttpHandler<File> handler = downloadInfo.getHandler();
        if (handler != null && !handler.isCancelled()) {
            handler.cancel();
        }
        mDownloadInfoList.remove(downloadInfo);
        db.delete(downloadInfo);
    }

    public void stopDownload(int index) throws DbException {
        JiDownloadInfo downloadInfo = mDownloadInfoList.get(index);
        stopDownload(downloadInfo);
    }

    public void stopCurrentDownload(int adid) throws DbException {
        JiDownloadInfo downloadInfo;

        for (int i = 0; i < mDownloadInfoList.size(); i++) {
            downloadInfo = mDownloadInfoList.get(i);
            if (downloadInfo.getAdCode() == adid) {
                stopDownload(downloadInfo);
            }
        }

    }

    public void stopDownload(JiDownloadInfo downloadInfo) throws DbException {
        HttpHandler<File> handler = downloadInfo.getHandler();
        if (handler != null && !handler.isCancelled()) {
            handler.cancel();
        } else {
            downloadInfo.setState(HttpHandler.State.CANCELLED);
        }
        db.saveOrUpdate(downloadInfo);
    }

    public void stopAllDownload() throws DbException {
        for (JiDownloadInfo downloadInfo : mDownloadInfoList) {
            HttpHandler<File> handler = downloadInfo.getHandler();
            if (handler != null && !handler.isCancelled()) {
                handler.cancel();
            } else {
                downloadInfo.setState(HttpHandler.State.CANCELLED);
            }
        }
        db.saveOrUpdateAll(mDownloadInfoList);
    }

    public void backupDownloadInfoList() throws DbException {
        for (JiDownloadInfo downloadInfo : mDownloadInfoList) {
            HttpHandler<File> handler = downloadInfo.getHandler();
            if (handler != null) {
                downloadInfo.setState(handler.getState());
            }
        }
        db.saveOrUpdateAll(mDownloadInfoList);
    }

    public int getMaxDownloadThread() {
        return maxDownloadThread;
    }

    public void setMaxDownloadThread(int maxDownloadThread) {
        this.maxDownloadThread = maxDownloadThread;
    }

    public class ManagerCallBack extends RequestCallBack<File> {
        private JiDownloadInfo downloadInfo;
        private RequestCallBack<File> baseCallBack;

        public RequestCallBack<File> getBaseCallBack() {
            return baseCallBack;
        }

        public void setBaseCallBack(RequestCallBack<File> baseCallBack) {
            this.baseCallBack = baseCallBack;
        }

        private ManagerCallBack(JiDownloadInfo jiDownloadInfo, RequestCallBack<File> baseCallBack) {
            this.baseCallBack = baseCallBack;
            this.downloadInfo = jiDownloadInfo;
        }

        @Override
        public Object getUserTag() {
            if (baseCallBack == null) return null;
            return baseCallBack.getUserTag();
        }

        @Override
        public void setUserTag(Object userTag) {
            if (baseCallBack == null) return;
            baseCallBack.setUserTag(userTag);
        }

        @Override
        public void onStart() {
            HttpHandler<File> handler = downloadInfo.getHandler();
            if (handler != null) {
                downloadInfo.setState(handler.getState());
                Log.e(TAG, "download onStart");
            }
            try {
                db.saveOrUpdate(downloadInfo);
            } catch (DbException e) {
                LogUtils.e(e.getMessage(), e);
            }
            if (baseCallBack != null) {
                baseCallBack.onStart();
            }
        }

        @Override
        public void onCancelled() {
            HttpHandler<File> handler = downloadInfo.getHandler();
            if (handler != null) {
                downloadInfo.setState(handler.getState());
                Log.e(TAG, "download onCancelled");
            }
            try {
                db.saveOrUpdate(downloadInfo);
            } catch (DbException e) {
                LogUtils.e(e.getMessage(), e);
            }
            if (baseCallBack != null) {
                baseCallBack.onCancelled();
            }
        }

        @Override
        public void onLoading(long total, long current, boolean isUploading) {
            HttpHandler<File> handler = downloadInfo.getHandler();
            if (handler != null) {
                downloadInfo.setState(handler.getState());
            }

            downloadInfo.setFileLength(total);
            downloadInfo.setProgress(current);

            try {
                db.saveOrUpdate(downloadInfo);
            } catch (DbException e) {
                LogUtils.e(e.getMessage(), e);
            }
            if (baseCallBack != null) {
                baseCallBack.onLoading(total, current, isUploading);
            }

            if (mShowNotify) {
                JiNotifyCompat notifyCompat = mNotifyCompatMap.get(mDownloadurl);
                notifyCompat.builder.setProgress((int) total, (int) current, false);
                try {
                    notifyCompat.builder.setContentText(formatSize(current) + "/" +
                            formatSize(total) +

                            " (" + current * 100 / total + "%)");
                } catch (ArithmeticException e) {
                    e.printStackTrace();
                }

                mNotificationManager.notify(notifyCompat.id, notifyCompat.builder.build());
            }
        }

        @Override
        public void onSuccess(ResponseInfo<File> responseInfo) {
            HttpHandler<File> handler = downloadInfo.getHandler();
            if (handler != null) {
                downloadInfo.setState(handler.getState());
                Log.e(TAG, "download onSuccess");
            }
            try {
                db.saveOrUpdate(downloadInfo);
            } catch (DbException e) {
                LogUtils.e(e.getMessage(), e);
            }
            if (baseCallBack != null) {
                baseCallBack.onSuccess(responseInfo);
            }

            JiNotifyCompat notifyCompat = mNotifyCompatMap.get(mDownloadurl);
            if (notifyCompat != null) {
                mNotificationManager.cancel(notifyCompat.id);
            }
        }

        @Override
        public void onFailure(HttpException error, String msg) {
            HttpHandler<File> handler = downloadInfo.getHandler();
            if (handler != null) {
                downloadInfo.setState(handler.getState());
                Log.e(TAG, "download onFailure");
            }
            try {
                db.saveOrUpdate(downloadInfo);
            } catch (DbException e) {
                LogUtils.e(e.getMessage(), e);
            }
            if (baseCallBack != null) {
                baseCallBack.onFailure(error, msg);
            }

            JiNotifyCompat notifyCompat = mNotifyCompatMap.get(mDownloadurl);

            if (notifyCompat != null) {
                mNotificationManager.cancel(notifyCompat.id);
            }
        }
    }

    private class HttpHandlerStateConverter implements ColumnConverter<HttpHandler.State> {

        @Override
        public HttpHandler.State getFieldValue(Cursor cursor, int index) {
            return HttpHandler.State.valueOf(cursor.getInt(index));
        }

        @Override
        public HttpHandler.State getFieldValue(String fieldStringValue) {
            if (fieldStringValue == null) return null;
            return HttpHandler.State.valueOf(fieldStringValue);
        }

        @Override
        public Object fieldValue2ColumnValue(HttpHandler.State fieldValue) {
            if (fieldValue == null) return null;
            return fieldValue.value();
        }

        @Override
        public ColumnDbType getColumnDbType() {
            return ColumnDbType.INTEGER;
        }
    }

    private int getNotifyId() {
        if (sNotifyId < 0) {
            sNotifyId = 100;
        }

        sNotifyId += 1;
        return sNotifyId;
    }

    private String formatSize(long totalBytes) {
        if (totalBytes >= 1000000) {
            return ((String.format("%.1f", (float) totalBytes / 1000000)) + "MB");
        }
        if (totalBytes >= 1000) {
            return ((String.format("%.1f", (float) totalBytes / 1000)) + "KB");
        } else {
            return (totalBytes + "Bytes");
        }
    }
}
