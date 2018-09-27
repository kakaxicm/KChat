package com.qicode.kakaxicm.kchat.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.qicode.kakaxicm.kchat.R;
import com.qicode.kakaxicm.kchat.constant.BroadcastConstant;
import com.qicode.kakaxicm.kchat.constant.UrlConstant;
import com.qicode.kakaxicm.kchat.listener.NetCallBack;
import com.qicode.kakaxicm.kchat.manager.NetManager;
import com.qicode.kakaxicm.kchat.model.MessageEvent;
import com.qicode.kakaxicm.kchat.model.UserModel;
import com.qicode.kakaxicm.kchat.ui.activity.BaseActivity;
import com.qicode.kakaxicm.kchat.ui.activity.ChatActivity;
import com.qicode.kakaxicm.kchat.util.KUtil;
import com.zhy.adapter.abslistview.CommonAdapter;
import com.zhy.adapter.abslistview.ViewHolder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

/**
 * Created by chenming on 2018/9/22
 */
public class HomeContactFragment extends BaseFragment {
    List<UserModel> mUsers = new ArrayList<UserModel>();

    @BindView(R.id.contact_list_view)
    ListView mListView;

    private CommonAdapter mAdapter;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_home_contact;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EventBus.getDefault().register(this);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void initView() {
        super.initView();

        mAdapter = new CommonAdapter<UserModel>(mContext, R.layout.contact_list_item, mUsers) {
            @Override
            protected void convert(ViewHolder viewHolder, UserModel item, int position) {

                viewHolder.setText(R.id.contact_user_name, item.getName());
                viewHolder.setText(R.id.contact_user_state, item.isOnline() ? "[在线]" : "[离线]");
            }
        };

        mListView.setAdapter(mAdapter);


        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                BaseActivity activity = (BaseActivity) mContext;

                Bundle bundle = new Bundle();
                bundle.putString("username", mUsers.get(i).getName());
                //TODO 跳转聊天界面
                activity.openActivity(ChatActivity.class, bundle);

                clearUnread(i);
            }
        });
    }


    /**
     * 清除未读
     *
     * @param position
     */
    private void clearUnread(int position) {
        // 发送一个进入聊天的通知，让消息列表界面刷新UI
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);

        UserModel user = mUsers.get(position);
        String conversationId = user.getName();
        Intent intent = new Intent(BroadcastConstant.clearUnreadMessage);
        intent.putExtra("conversation", conversationId);
        localBroadcastManager.sendBroadcast(intent);
    }

    @Override
    protected void initData() {
        super.initData();

        requestData();
    }

    /**
     * 请求用户列表
     */
    private void requestData() {

        NetManager.get(mContext, UrlConstant.allUsers_url, null, new NetCallBack() {
            @Override
            public void onSuccess(String data) {

                JSONObject jsonObject = JSON.parseObject(data).getJSONObject("data");

                List<UserModel> allUsers = JSON.parseArray(jsonObject.getJSONArray("allUser").toJSONString(), UserModel.class);

                List<String> onlineUsers = JSON.parseArray(jsonObject.getJSONArray("onLineUsers").toString(), String.class);

                for (String onlineUser : onlineUsers) {

                    for (UserModel user : allUsers) {

                        if (user.getName().equals(onlineUser)) {

                            user.setOnline(true);
                            break;
                        }
                    }
                }
                mUsers.clear();
                mUsers.addAll(allUsers);
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError() {

                KUtil.showShortToast(mContext, "查询用户失败！");
            }

            @Override
            public void closeProgressHud() {

            }
        });

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocketEvent(MessageEvent event) {

        switch (event.getType()) {
            case EventUserOnline: // 用户上线

                String name = (String) event.getMsg();
                for (UserModel user : mUsers) {

                    if (user.getName().equals(name)){
                        user.setOnline(true);

                        break;
                    }
                }
                KUtil.showShortToast(mContext, name + "上线了");

                mAdapter.notifyDataSetChanged();
                break;

            case EventUserOffLine:

                String name1 = (String) event.getMsg();
                for (UserModel user : mUsers) {

                    if (user.getName().equals(name1)){
                        user.setOnline(false);

                        break;
                    }
                }
                KUtil.showShortToast(mContext, name1 + "下线了");
                mAdapter.notifyDataSetChanged();
                break;
        }
    }

}
