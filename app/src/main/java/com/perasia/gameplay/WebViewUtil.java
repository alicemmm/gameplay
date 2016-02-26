package com.perasia.gameplay;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.perasia.gameplay.download.JiDownloadManager;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebViewUtil {
    private static final String TAG = WebViewUtil.class.getSimpleName();

    private Context mContext;

    private WebView mWebView;
    private WebSettings mWebSettings;

    private JiDownloadManager mDownloadManager;

    private WebViewCallBack mCallBack;

    private WebViewUtil(Context context, WebView webView, WebViewCallBack callBack) {
        mContext = context;
        mWebView = webView;
        mCallBack = callBack;
        if (mWebView != null) {
            mWebSettings = mWebView.getSettings();
        }

        if (mWebSettings != null) {
            initWebView();
        }
    }

    public static WebViewUtil getInstance(Context context, WebView webView, WebViewCallBack callBack) {
        return new WebViewUtil(context, webView, callBack);
    }

    private void initWebView() {
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setSupportZoom(true);
        mWebSettings.setBuiltInZoomControls(false);
        mWebView.requestFocus();

        MyWebViewClient myWebViewClient = new MyWebViewClient();
        mWebView.setWebViewClient(myWebViewClient);

        ChromeClient webChromeClient = new ChromeClient();
        mWebView.setWebChromeClient(webChromeClient);

        if (Build.VERSION.SDK_INT >= 19) {
            mWebSettings.setLoadsImagesAutomatically(true);
        } else {
            mWebSettings.setLoadsImagesAutomatically(false);
        }

        mWebSettings.setBlockNetworkImage(true);

        mWebSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        setWebViewDownloadListener();
    }

    private void setWebViewDownloadListener() {
        mWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String s1, String s2, String s3, long l) {
                String regEx = "[^0-9]";
                Pattern p = Pattern.compile(regEx);
                Matcher m = p.matcher(url);

                final String savePath;
                if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                        || !Environment.isExternalStorageRemovable()) {
                    savePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + m.replaceAll("").trim() + ".apk";
                } else {
                    savePath = mContext.getCacheDir().getPath() + m.replaceAll("").trim() + ".apk";
                }

                mDownloadManager = new JiDownloadManager(mContext);
                if (isFileExist(savePath)) {
                    deleteFile(savePath);
                }
                try {
                    mDownloadManager.addNewDownload(url, null, savePath, 0,
                            false, false, true, new RequestCallBack<File>() {
                                @Override
                                public void onStart() {
                                    super.onStart();
                                    Log.e(TAG, "download start");
                                }

                                @Override
                                public void onSuccess(ResponseInfo<File> fileResponseInfo) {
                                    installApk(mContext, savePath);
                                }

                                @Override
                                public void onFailure(HttpException e, String s) {
                                    Log.e(TAG, s);
                                }

                            });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public void installApk(Context context, String filePath) {
        if (context == null || TextUtils.isEmpty(filePath)) {
            return;
        }

        if (!checkAppPackage(context, filePath)) {
            return;
        }

        try {
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            intent.addCategory("android.intent.category.DEFAULT");
            intent.setDataAndType(Uri.fromFile(new File(filePath)), "application/vnd.android.package-archive");
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkAppPackage(Context context, String filePath) {
        if (context == null || TextUtils.isEmpty(filePath)) {
            return false;
        }

        boolean result = true;
        try {
            context.getPackageManager().getPackageArchiveInfo(filePath, PackageManager.GET_ACTIVITIES);
        } catch (Exception e) {
            deleteFile(filePath);
            result = false;
        }

        return result;
    }

    private boolean isFileExist(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return false;
        }
        File file = new File(fileName);
        return file.exists();
    }

    private void deleteFile(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return;
        }
        File file = new File(filePath);
        file.delete();
    }


    /**
     * 通过自己的Webview来显示所有网页
     */
    private class MyWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (mCallBack != null) {
                mCallBack.shouldOverrideUrlLoading(view, url);
            }
            return false;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (!mWebSettings.getLoadsImagesAutomatically()) {
                mWebSettings.setLoadsImagesAutomatically(true);
            }
            mWebSettings.setBlockNetworkImage(false);

            if (mCallBack != null) {
                mCallBack.onPageFinished(view, url);
            }

            super.onPageFinished(view, url);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (mCallBack != null) {
                mCallBack.onPageStarted(view, url, favicon);
            }

            super.onPageStarted(view, url, favicon);
        }
    }

    /**
     * WebChromeClient自定义继承类,获取标题，获得当前进度
     */
    private class ChromeClient extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if (mCallBack != null) {
                mCallBack.onProgressChanged(view, newProgress);
            }
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            if (mCallBack != null) {
                mCallBack.onReceivedTitle(view, title);
            }
        }
    }

}
