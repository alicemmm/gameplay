package com.perasia.gameplay.download2;


import android.util.Log;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadManager {
    private static final String TAG = DownloadManager.class.getSimpleName();

    public interface DownloadCallBack {
        void onStart(int fileSize);

        void onLoading(int fileSize, int downloadSize);

        void onFinished();
    }

    private DownloadCallBack callBack;

    private static DownloadManager downloadManager;

    private DownloadManager(String downloadUrl, int threadNum, String filePath, DownloadCallBack callBack) {
        this.callBack = callBack;
        DownloadTask task = new DownloadTask(downloadUrl, threadNum, filePath);
        task.start();
    }

    public static DownloadManager download(String downloadUrl, int threadNum, String filepath, DownloadCallBack callBack) {
        if (downloadManager == null) {
            downloadManager = new DownloadManager(downloadUrl, threadNum, filepath, callBack);
        }

        return downloadManager;
    }

    private class DownloadTask extends Thread {
        private String downloadUrl;
        private int threadNum;
        private String filePath;
        private int blockSize;

        public DownloadTask(String downloadUrl, int threadNum, String filePath) {
            this.downloadUrl = downloadUrl;
            this.threadNum = threadNum;
            this.filePath = filePath;
        }

        @Override
        public void run() {
            DownloadThread[] threads = new DownloadThread[threadNum];
            try {
                URL url = new URL(downloadUrl);
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                int fileSize = conn.getContentLength();
                if (fileSize <= 0) {
                    Log.e(TAG, "read file failed");
                    return;
                }

                if (callBack != null) {
                    callBack.onStart(fileSize);
                }

                //get file max size
                blockSize = (fileSize % threadNum) == 0 ? fileSize / threadNum : fileSize / threadNum + 1;

                Log.e(TAG, "fileSize:" + fileSize + "  blockSize:");

                File file = new File(filePath);
                for (int i = 0; i < threads.length; i++) {
                    threads[i] = new DownloadThread(url, file, blockSize, (i + 1));
                    threads[i].setName("Thread:" + i);
                    threads[i].start();
                }

                boolean isFinished = false;

                int downloadAllSize = 0;

                while (!isFinished) {
                    isFinished = true;
                    downloadAllSize = 0;
                    for (int i = 0; i < threads.length; ++i) {
                        downloadAllSize += threads[i].getDownloadLength();

                        if (!threads[i].isCompleted()) {
                            isFinished = false;
                        }
                    }

                    if (callBack != null) {
                        callBack.onLoading(fileSize, downloadAllSize);
                        Thread.sleep(1000);
                    }
                }

                Log.e(TAG, " all of downloadSize:" + downloadAllSize);

                if (downloadAllSize == fileSize) {
                    if (callBack != null) {
                        callBack.onFinished();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
