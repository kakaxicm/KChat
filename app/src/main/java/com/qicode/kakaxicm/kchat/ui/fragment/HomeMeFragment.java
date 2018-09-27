package com.qicode.kakaxicm.kchat.ui.fragment;

import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.blankj.utilcode.util.SPUtils;
import com.qicode.kakaxicm.kchat.R;
import com.qicode.kakaxicm.kchat.constant.UrlConstant;
import com.qicode.kakaxicm.kchat.db.DbManager;
import com.qicode.kakaxicm.kchat.enums.MessageEventType;
import com.qicode.kakaxicm.kchat.enums.SocketConnectStatus;
import com.qicode.kakaxicm.kchat.listener.NetCallBack;
import com.qicode.kakaxicm.kchat.manager.ClientManager;
import com.qicode.kakaxicm.kchat.manager.NetManager;
import com.qicode.kakaxicm.kchat.manager.SocketManager;
import com.qicode.kakaxicm.kchat.model.MessageEvent;
import com.qicode.kakaxicm.kchat.util.KLog;
import com.qicode.kakaxicm.kchat.util.KUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;


/**
 * Created by chenming on 2018/9/22
 */
public class HomeMeFragment extends BaseFragment {
    @BindView(R.id.me_current_user)
    TextView currentUserView;

    @BindView(R.id.me_connect_status)
    TextView connectStatusView;

    @OnClick(R.id.me_exit_login)
    void exitLogin() {
        if (SocketManager.connectStatus == SocketConnectStatus.SocketConnected) {
            SocketManager.socket.disconnect();

        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_home_me;
    }

    @Override
    protected void initView() {
        super.initView();

        currentUserView.setText("登录用户：" + ClientManager.currentUserId);

        connectStatusView.setText(connectStatusDes(SocketManager.connectStatus));

    }

    private String connectStatusDes(SocketConnectStatus status) {
        String des = "";
        switch (status) {
            case SocketConnected:
                des = "连接成功";
                break;
            case SocketConnecting:
                des = "连接中";
                break;
            case SocketConnectError:
                des = "连接失败";
                break;
            case SocketDisconnected:
                des = "连接断开";
                break;
        }
        return "连接状态：" + des;
    }

    @Override
    protected void initData() {
        super.initData();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    /**
     * 处理socket连接发生变化的事件
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocketEvent(MessageEvent event) {

        switch (event.getType()) {
            case EventConnectStatus:
                SocketConnectStatus status = (SocketConnectStatus) event.getMsg();
                connectStatusView.setText(connectStatusDes(status));
                break;
        }
    }

    @OnClick(R.id.me_connect)
    public void onLogInClick() {
        //获取当前的socket连接状态
        if (SocketManager.connectStatus == SocketConnectStatus.SocketConnected) {
            KUtil.showShortToast(mContext, "您已经处在登陆状态");
            return;
        }
        Map parameters = new HashMap();
        final String user = SPUtils.getInstance().getString("username");
        final String pwd = SPUtils.getInstance().getString("password");
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

                            ClientManager.currentUserId = user;
                            // 连接成功，创建数据库
                            DbManager.getsInstance().createDb(mContext.getApplicationContext(), user);
                            EventBus.getDefault().post(new MessageEvent(MessageEventType.EventConnectStatus, SocketConnectStatus.SocketConnected));
                        }

                        @Override
                        public void fail() {

                        }
                    };
                    KLog.i("获取登录信息成功");
                    SocketManager.connect(mContext.getApplicationContext(), auth_token, callBack);

                }
            }

            @Override
            public void onError() {

                KUtil.showShortToast(mContext, "登录失败！");
            }

            @Override
            public void closeProgressHud() {

            }
        });
    }
}
