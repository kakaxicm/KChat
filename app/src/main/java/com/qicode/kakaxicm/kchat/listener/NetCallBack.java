package com.qicode.kakaxicm.kchat.listener;

/**
 * Created by chenming on 2018/9/20
 */
public abstract class NetCallBack {
    public abstract void onSuccess(String data);

    public abstract void onError();

    public abstract void closeProgressHud();
}
