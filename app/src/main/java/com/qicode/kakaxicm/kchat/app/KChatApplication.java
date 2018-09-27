package com.qicode.kakaxicm.kchat.app;

import android.app.Application;

import com.blankj.utilcode.util.Utils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.cookie.CookieJarImpl;
import com.zhy.http.okhttp.cookie.store.PersistentCookieStore;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by chenming on 2018/9/20
 */
public class KChatApplication extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        Utils.init(this);
        initOkHttpUtil();
    }

    /*
     * 初始化网络请求管理器
     * */
    private void initOkHttpUtil () {

        CookieJarImpl cookieJar = new CookieJarImpl(new PersistentCookieStore(getApplicationContext()));
        OkHttpClient client = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {

                        Request request = chain.request();
                        request = request.newBuilder().addHeader("Connection", "keep-alive").build();
                        return chain.proceed(request);
                    }
                })
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .cookieJar(cookieJar)
                .build();
        OkHttpUtils.initClient(client);
    }

}
