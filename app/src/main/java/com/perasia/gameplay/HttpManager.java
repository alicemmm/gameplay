package com.perasia.gameplay;


import android.util.Log;

import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class HttpManager {
    private static final String TAG = HttpManager.class.getSimpleName();

    public interface HttpCallBack {
        void onSuccess(Map<Integer, String> map);

        void onFailure(String msg);
    }

    private HttpCallBack callBack;
    private String url;

    public HttpManager(String url) {
        this.url = url;
    }

    public HttpManager(String url, final HttpCallBack callBack) {
        this(url);
        this.callBack = callBack;
    }

    public void send() {
        HttpUtils http = new HttpUtils();
        http.send(HttpRequest.HttpMethod.GET, url, new RequestCallBack<String>() {
            @Override
            public void onSuccess(ResponseInfo<String> responseInfo) {
                String res = responseInfo.result;
                analyseResult(res);
            }

            @Override
            public void onFailure(HttpException error, String msg) {
                if (callBack != null) {
                    callBack.onFailure(msg);
                }
            }
        });
    }

    private void analyseResult(String res) {
        Map<Integer, String> map = new HashMap<>();

        Log.e(TAG, "res=" + res);

        try {
            JSONObject jsonObject = new JSONObject(res);
            map.put(1, jsonObject.getString("1"));
            map.put(2, jsonObject.getString("2"));
            map.put(3, jsonObject.getString("3"));
            map.put(4, jsonObject.getString("4"));

            if (callBack != null) {
                callBack.onSuccess(map);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            if (callBack != null) {
                callBack.onFailure("JSONException");
            }
        }
    }

}
