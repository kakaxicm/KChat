package com.qicode.kakaxicm.kchat.manager;

import android.content.Context;

import com.qicode.kakaxicm.kchat.constant.UrlConstant;
import com.qicode.kakaxicm.kchat.listener.NetCallBack;
import com.qicode.kakaxicm.kchat.util.KUtil;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.util.Map;

import okhttp3.Call;

/**
 * Created by chenming on 2018/9/20
 */
public class NetManager {

    public enum RequestType {
        Get, Post
    }

    public static void get(Context context, String url, Map<String, String> parameters, final NetCallBack callBack) {

        request(RequestType.Get, context, url, parameters, callBack);
    }

    public static void post(Context context, String url, Map<String, String> parameters, NetCallBack callBack) {

        request(RequestType.Post, context, url, parameters, callBack);
    }

    private static void request(RequestType requestType, Context context, String url, Map<String, String> parameters, final NetCallBack callBack) {
        if (!KUtil.netIsConnect(context)) {

            KUtil.showShortToast(context, "网络未连接！");
            return;
        }
        final String requestUrl = UrlConstant.baseUrl + "/" + url;
        if (requestType == RequestType.Get) {

            OkHttpUtils.get()
                    .tag(context)
                    .url(requestUrl)
                    .params(parameters)
                    .build()
                    .execute(new StringCallback() {
                        @Override
                        public void onError(Call call, Exception e, int id) {

                            callBack.closeProgressHud();
                            callBack.onError();
                        }

                        @Override
                        public void onResponse(String response, int id) {

                            callBack.closeProgressHud();
                            callBack.onSuccess(response);
                        }
                    });
        } else if (requestType == RequestType.Post) {

            OkHttpUtils.post()
                    .url(requestUrl)
                    .tag(context)
                    .params(parameters)
                    .build()
                    .execute(new StringCallback() {
                        @Override
                        public void onError(Call call, Exception e, int id) {

                            callBack.closeProgressHud();
                            callBack.onError();
                        }

                        @Override
                        public void onResponse(String response, int id) {

                            callBack.closeProgressHud();
                            callBack.onSuccess(response);
                        }
                    });
        }

    }
}
