package com.perasia.gameplay.download2;


import android.util.Log;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DownloadManager {
    private static final String TAG = DownloadManager.class.getSimpleName();

    public interface DownloadCallBack {
        void onStart(int fileSize);

        void onLoading(int fileSize, int downloadSize);

        void onFinished();
    }

    private DownloadCallBack callBack;

    private static DownloadManager downloadManager;

    private static List<String> lists = new ArrayList<>();

    public static DownloadManager getInstance() {
        if (downloadManager == null) {
            downloadManager = new DownloadManager();
        }

        return downloadManager;
    }

    public void execute(String downloadUrl, int threadNum, String filePath, DownloadCallBack callBack) {
        if (lists.contains(filePath)) {
            Log.e(TAG, "downloading");
            return;
        }

        lists.add(filePath);

        this.callBack = callBack;
        DownloadTask task = new DownloadTask(downloadUrl, threadNum, filePath);
        task.start();
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
            HttpURLConnection conn = null;
            try {
                URL url = new URL(downloadUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5 * 1000);
                int fileSize = conn.getContentLength();
                if (fileSize <= 0) {
                    Log.e(TAG, "read file failed");
                    return;
                }

                if (callBack != null) {
                    callBack.onStart(fileSize);
                }

                RandomAccessFile accessFile = new RandomAccessFile(filePath, "rwd");
                accessFile.setLength(fileSize);
                accessFile.close();

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
                    lists.remove(filePath);

                    if (callBack != null) {
                        callBack.onFinished();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
    }

}
