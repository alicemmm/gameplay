package com.perasia.gameplay;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private Context mContext;

    private ActionBar mActionBar;

    private WebView mWebView;

    private ProgressBar mProgressBar;

    private ImageView mImageView;

    private Button mBtn;

    private String mUrl;

    private boolean isWebLoaded = false;

    private int mCount;
    private long mLastTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        mWebView = (WebView) findViewById(R.id.main_webview);
        mImageView = (ImageView) findViewById(R.id.main_iv);
        mBtn = (Button) findViewById(R.id.main_btn);
        mProgressBar = (ProgressBar) findViewById(R.id.webProgress_pb);

        mUrl = "http://192.168.5.110:8080/bunengsi/index.html";

        init();

    }

    private void init() {
        mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setTitle(R.string.action_title);
        }

        WebViewUtil.getInstance(mContext, mWebView, new WebViewCallBack() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {

            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {

            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {
                    mProgressBar.setVisibility(View.GONE);
                    isWebLoaded = true;
                } else {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mProgressBar.setProgress(newProgress);
                    isWebLoaded = false;
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {

            }
        });

        mWebView.loadUrl(mUrl);

//        PushUtils.init(mContext, R.mipmap.ic_launcher);
//        JiAdUtil.init(mContext, mImageView);

        // TODO: 16/2/29 test download
        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String savePath;

                String url = "http://jsp.dx1200.com/apk/2016/dyj_311_3.0.0.apk";

                savePath = CommonUtil.getDownloadSavePath(mContext, url);
                DownloadManager downloadManager = new DownloadManager(mContext);

                downloadManager.execute(url, savePath, true, new DownloadManager.DownloadCallBack() {
                    @Override
                    public void onStart() {
                        Log.e(TAG, "onStart");
                        Toast.makeText(mContext, R.string.start_download, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onLoading(long total, long current, boolean isUploading) {
                        Log.e(TAG, "onLoading=" + (current * 100) / total + "%");
                    }

                    @Override
                    public void onSuccess(ResponseInfo<File> responseInfo) {
                        Log.e(TAG, "onSuccess");
                        CommonUtil.installApk(mContext, savePath);
                    }

                    @Override
                    public void onFailure(HttpException error, String msg) {
                        Log.e(TAG, "onFailure");
                    }
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            mWebView.getClass().getMethod("onResume").invoke(mWebView, (Object[]) null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            mWebView.getClass().getMethod("onPause").invoke(mWebView, (Object[]) null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_refresh) {
            if (isWebLoaded) {
                mWebView.loadUrl(mUrl);
            } else {
                mWebView.stopLoading();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            long curTime = System.currentTimeMillis();
            if (mLastTime + 2000 > curTime) {
                mCount += 1;
            } else {
                mCount = 1;
            }
            mLastTime = curTime;

            if (mCount < 2) {
                Toast.makeText(mContext, R.string.main_back_exit, Toast.LENGTH_SHORT).show();
                return false;
            }

            if (mCount >= 2) {
                return super.onKeyDown(keyCode, event);
            }
        }

        return false;
    }

}
