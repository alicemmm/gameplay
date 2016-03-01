package com.perasia.gameplay;


import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;

import java.io.File;

public class WebViewUtil {
    private static final String TAG = WebViewUtil.class.getSimpleName();

    private Context mContext;

    private WebView mWebView;
    private WebSettings mWebSettings;

    private DownloadManager mDownloadManager;

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
                final String savePath = CommonUtil.getDownloadSavePath(mContext, url);

                mDownloadManager = DownloadManager.getInstance(mContext);

                mDownloadManager.execute(url, savePath, true, new DownloadManager.DownloadCallBack() {
                    @Override
                    public void onStart() {
                        Log.e(TAG, "download start");
                    }

                    @Override
                    public void onLoading(long total, long current, boolean isUploading) {

                    }

                    @Override
                    public void onSuccess(ResponseInfo<File> responseInfo) {
                        CommonUtil.installApk(mContext, savePath);
                    }

                    @Override
                    public void onFailure(HttpException error, String msg) {
                        Log.e(TAG, msg);
                    }
                });

            }
        });
    }

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
