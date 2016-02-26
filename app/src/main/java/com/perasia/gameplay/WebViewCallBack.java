package com.perasia.gameplay;


import android.graphics.Bitmap;
import android.webkit.WebView;

public interface WebViewCallBack {
    boolean shouldOverrideUrlLoading(WebView view, String url);

    void onPageFinished(WebView view, String url);

    void onPageStarted(WebView view, String url, Bitmap favicon);

    void onProgressChanged(WebView view, int newProgress);

    void onReceivedTitle(WebView view, String title);
}
