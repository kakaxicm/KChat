package com.qicode.kakaxicm.kchat.ui.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.view.MotionEvent;
import android.widget.EditText;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.blankj.utilcode.util.KeyboardUtils;
import com.blankj.utilcode.util.SPUtils;
import com.qicode.kakaxicm.kchat.R;
import com.qicode.kakaxicm.kchat.constant.UrlConstant;
import com.qicode.kakaxicm.kchat.db.DbManager;
import com.qicode.kakaxicm.kchat.listener.NetCallBack;
import com.qicode.kakaxicm.kchat.manager.ClientManager;
import com.qicode.kakaxicm.kchat.manager.NetManager;
import com.qicode.kakaxicm.kchat.manager.SocketManager;
import com.qicode.kakaxicm.kchat.util.KLog;
import com.qicode.kakaxicm.kchat.util.KUtil;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by chenming on 2018/9/20
 */
public class LoginActivity extends BaseActivity {

    @BindView(R.id.login_username)
    EditText userName;

    @BindView(R.id.login_password)
    EditText password;

    @BindView(R.id.login_loading_view)
    AVLoadingIndicatorView loadingView;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_login;
    }


    @Override
    protected void initView() {
        super.initView();


        userName.setText(SPUtils.getInstance().getString("username"));
        password.setText(SPUtils.getInstance().getString("password"));
    }

    @Override
    protected void initData() {
        super.initData();
        checkPermissions();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        KeyboardUtils.hideSoftInput(this);
        return super.onTouchEvent(event);
    }


    private void checkPermissions() {

        int REQUEST_EXTERNAL_STORAGE = 1;
        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.MODIFY_AUDIO_SETTINGS
        };
        int permission = ActivityCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    private boolean checkInput() {
        String user = userName.getText().toString();
        String pwd = password.getText().toString();
        if (user.isEmpty()) {

            KUtil.showShortToast(mContext, "请输入用户名...");
            return false;
        } else if (pwd.isEmpty()) {
            KUtil.showShortToast(mContext, "请输入密码...");
            return false;
        } else {
            return true;
        }
    }


    @OnClick(R.id.btn_login)
    void login() {
        if (checkInput()) {

            loadingView.show();
            Map parameters = new HashMap();
            final String user = userName.getText().toString();
            final String pwd = password.getText().toString();
            parameters.put("userName", user);
            parameters.put("password", pwd);
            NetManager.post(mContext, UrlConstant.login_url, parameters, new NetCallBack() {
                @Override
                public void onSuccess(String data) {

                    JSONObject object = JSON.parseObject(data);
                    if (object.getInteger("code") <= 0) {

                        String message = object.getString("message");
                        KUtil.showShortToast(mContext, message);
                    } else {

                        String auth_token = object.getJSONObject("data").getString("auth_token");
                        UrlConstant.auth_token = auth_token;

                        SPUtils.getInstance().put("username", user);
                        SPUtils.getInstance().put("password", pwd);

                        SocketManager.SocketCallBack callBack = new SocketManager.SocketCallBack() {

                            @Override
                            public void success() {

                                KLog.i("socket连接成功");
                                //当前登录用户保存
                                ClientManager.currentUserId = user;
                                // 连接成功，创建数据库
                                // TODO revert
                                DbManager.getsInstance().createDb(getApplicationContext(), user);
                                // TODO revert 打开主页
                                openActivity(HomeActivity.class);
                                finish();
                            }

                            @Override
                            public void fail() {

                            }
                        };
                        KLog.i("获取登录信息成功");
                        SocketManager.connect(getApplicationContext(), auth_token, callBack);
                    }
                }

                @Override
                public void onError() {

                    KUtil.showShortToast(mContext, "登录失败！");
                }

                @Override
                public void closeProgressHud() {

                    loadingView.hide();
                }
            });
        }
    }

    @OnClick(R.id.btn_register)
    void register() {

        if (checkInput()) {
            loadingView.show();
            Map parameters = new HashMap();
            final String user = userName.getText().toString();
            final String pwd = password.getText().toString();
            parameters.put("userName", user);
            parameters.put("password", pwd);
            NetManager.post(mContext, UrlConstant.register_url, parameters, new NetCallBack() {
                @Override
                public void onSuccess(String data) {

                    JSONObject object = JSON.parseObject(data);
                    if (object.getInteger("code") < 0) {
                        String message = object.getString("message");
                        KUtil.showShortToast(mContext, message);
                    } else {
                        KUtil.showShortToast(mContext, "注册成功");
                    }
                }

                @Override
                public void onError() {

                    KUtil.showShortToast(mContext, "注册失败");
                }

                @Override
                public void closeProgressHud() {

                    loadingView.hide();
                }
            });
        }
    }
}
