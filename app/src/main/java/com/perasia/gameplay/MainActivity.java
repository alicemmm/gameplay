package com.perasia.gameplay;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;

import com.perasia.gameplay.download2.DownloadManager;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private Context mContext;

    private ActionBar mActionBar;

    private WebView mWebView;

    private ImageView mImageView;

    private Button mBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        mWebView = (WebView) findViewById(R.id.main_webview);
        mImageView = (ImageView) findViewById(R.id.main_iv);
        mBtn = (Button) findViewById(R.id.main_btn);

        init();
    }

    private void init() {
        mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setTitle("游戏");
        }

        WebViewUtil.getInstance(mContext, mWebView, new WebViewCallBack() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.e(TAG, "shouldOverrideUrlLoading");
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.e(TAG, "onPageFinished");
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.e(TAG, "onPageStarted");
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                Log.e(TAG, "onProgressChanged");
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                Log.e(TAG, "onReceivedTitle");
            }
        });

        mWebView.loadUrl("http://www.baidu.com");

//        PushUtils.init(mContext, R.mipmap.ic_launcher);
//        JiAdUtil.init(mContext, mImageView);

        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String savePath;
                if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                        || !Environment.isExternalStorageRemovable()) {
                    savePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "name" + ".apk";
                } else {
                    savePath = mContext.getCacheDir().getPath() + "name" + ".apk";
                }

                DownloadManager.download("http://jsp.dx1200.com/apk/2016/dyj_311_3.0.0.apk", 5, savePath, new DownloadManager.DownloadCallBack() {
                    @Override
                    public void onStart(int fileSize) {
                        Log.e(TAG, "onStart=" + fileSize);
                        if (isFileExist(savePath)) {
                            deleteMyFile(savePath);
                        }
                    }

                    @Override
                    public void onLoading(int fileSize, int downloadSize) {
//                        Log.e(TAG, "filesize+downloadsize=" + fileSize + "--" + downloadSize);

                        Log.e(TAG, "onLoading=" + (downloadSize * 100 / fileSize) + "%");
                    }

                    @Override
                    public void onFinished() {
                        Log.e(TAG, "onFinished=");
                        installApk(mContext, savePath);
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

        return super.onOptionsItemSelected(item);
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

    private void deleteMyFile(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return;
        }
        File file = new File(filePath);
        file.delete();
    }

}
