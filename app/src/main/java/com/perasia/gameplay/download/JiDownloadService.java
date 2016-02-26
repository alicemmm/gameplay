package com.perasia.gameplay.download;

import android.app.ActivityManager;
import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.lidroid.xutils.exception.DbException;
import com.lidroid.xutils.util.LogUtils;

import java.util.List;

public class JiDownloadService extends Service {

    private static JiDownloadManager DOWNLOAD_MANAGER;

    public static JiDownloadManager getDownloadManager(Context appContext) {
        if (appContext == null) {
            Application application=new Application();
            appContext = application.getApplicationContext();
        }

        if (!JiDownloadService.isServiceRunning(appContext)) {
            Intent downloadSvr = new Intent(appContext, JiDownloadService.class);
            appContext.startService(downloadSvr);
        }
        if (JiDownloadService.DOWNLOAD_MANAGER == null) {
            JiDownloadService.DOWNLOAD_MANAGER = new JiDownloadManager(appContext);
        }
        return DOWNLOAD_MANAGER;
    }

    public JiDownloadService() {
        super();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public void onDestroy() {
        if (DOWNLOAD_MANAGER != null) {
            try {
                DOWNLOAD_MANAGER.stopAllDownload();
                DOWNLOAD_MANAGER.backupDownloadInfoList();
            } catch (DbException e) {
                LogUtils.e(e.getMessage(), e);
            }
        }
        super.onDestroy();
    }

    public static boolean isServiceRunning(Context context) {
        boolean isRunning = false;

        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList
                = activityManager.getRunningServices(Integer.MAX_VALUE);

        if (serviceList == null || serviceList.size() == 0) {
            return false;
        }

        for (int i = 0; i < serviceList.size(); i++) {
            if (serviceList.get(i).service.getClassName().equals(JiDownloadService.class.getName())) {
                isRunning = true;
                break;
            }
        }
        return isRunning;
    }
}
