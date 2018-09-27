package com.qicode.kakaxicm.kchat.ui.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.qicode.kakaxicm.kchat.R;
import com.qicode.kakaxicm.kchat.constant.BroadcastConstant;
import com.qicode.kakaxicm.kchat.db.DbManager;
import com.qicode.kakaxicm.kchat.db.dbmodel.ConversationGreenDbObject;
import com.qicode.kakaxicm.kchat.manager.ClientManager;
import com.qicode.kakaxicm.kchat.model.MessageEvent;
import com.qicode.kakaxicm.kchat.model.MessageModel;
import com.qicode.kakaxicm.kchat.ui.activity.BaseActivity;
import com.qicode.kakaxicm.kchat.ui.activity.ChatActivity;
import com.qicode.kakaxicm.kchat.util.KLog;
import com.qicode.kakaxicm.kchat.util.TimeUtil;
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
public class HomeMessageFragment extends BaseFragment {
    private LocalBroadcastManager mLocalBroadcastManager;
    private MessageFragmentBroadcastReceiver mBroadcastReceiver;
    private IntentFilter intentFilter;

    List<ConversationGreenDbObject> mItems = new ArrayList<ConversationGreenDbObject>();
    private CommonAdapter mAdapter;

    @BindView(R.id.conversation_list_view)
    ListView mListView;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_home_message;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void initView() {
        super.initView();

        mAdapter = new CommonAdapter<ConversationGreenDbObject>(mContext, R.layout.coversation_list_item, mItems) {
            @Override
            protected void convert(ViewHolder viewHolder, ConversationGreenDbObject item, int position) {

                viewHolder.setText(R.id.list_user_name, item.getId());
                viewHolder.setText(R.id.list_content, item.getLatestmsgtext());

                String time = TimeUtil.getHourStrTime(item.getLatestmsgtimestamp());
                viewHolder.setText(R.id.list_time, time);
                viewHolder.setText(R.id.list_unreadCount, Integer.toString(item.getUnreadcount()));
                TextView unreadText = viewHolder.getView(R.id.list_unreadCount);
                if (item.getUnreadcount() == 0) {
                    unreadText.setVisibility(View.GONE);
                } else {
                    unreadText.setVisibility(View.VISIBLE);
                }
            }
        };
        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                BaseActivity activity = (BaseActivity) mContext;
                Bundle bundle = new Bundle();
                bundle.putString("username", mItems.get(i).getId());
                //TODO 跳转到 ChatActivity
                activity.openActivity(ChatActivity.class, bundle);


                // 清空未读消息
                clearUnread(i);
            }
        });
    }

    private void clearUnread(int position) {

        ConversationGreenDbObject conversation = mItems.get(position);
        // 清空数据库未读消息
        DbManager.getsInstance().clearConversationUnreadCount(conversation.getId());

        // 清空UI
        conversation.setUnreadcount(0);
        mAdapter.notifyDataSetChanged();
    }

    private void clearUnread(String conversationId) {

        int i = 0;
        for (ConversationGreenDbObject dbObject : mItems) {

            if (dbObject.getId().equals(conversationId)) {

                clearUnread(i);
                break;
            }
            i++;
        }
    }

    @Override
    protected void initData() {
        queryData();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        mBroadcastReceiver = new MessageFragmentBroadcastReceiver();
        intentFilter = new IntentFilter();

        intentFilter.addAction(BroadcastConstant.clearUnreadMessage);
        intentFilter.addAction(BroadcastConstant.updateConversation);

        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private void queryData() {

        // 查询会话数据
        DbManager.getsInstance().queryAllConversation(new DbManager.QueryDbCallBack<ConversationGreenDbObject>() {
            @Override
            public void querySuccess(List<ConversationGreenDbObject> items, boolean hasMore) {
                updateConversationList(items);
            }
        });
    }


    private void updateConversationList(List<ConversationGreenDbObject> dbDatas) {
        mItems.clear();
        mItems.addAll(dbDatas);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);


        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
    }

    //从socket收到事件
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocketMessageEvent(MessageEvent event) {

        Object object = event.getMsg();
        switch (event.getType()) {
            case EventMessage:
                receivedNewMessage((MessageModel) event.getMsg());
                break;
        }
    }

    /*
     * 收到新消息
     * */
    private void receivedNewMessage(MessageModel messageModel) {

        boolean hasEqual = false;
        //取会话的对方id
        String id = ClientManager.currentUserId.equals(messageModel.getFrom_user()) ? messageModel.getTo_user() : messageModel.getFrom_user();
        for (ConversationGreenDbObject conversation : mItems) {

            if (conversation.getId().equals(id)) {

                boolean isChatting = ClientManager.isChattingWithUser(id);
                conversation.setUnreadcount(isChatting ? 0 : conversation.getUnreadcount());
                conversation.setLatestmsgtimestamp(messageModel.getTimestamp());
                conversation.setLatestmsgtext(messageModel.getMessageTip());

                mItems.remove(conversation);
                mItems.add(0, conversation);

                hasEqual = true;
                break;
            }
        }
        //如果所有会话的对方id都不匹配，则新建会话
        if (!hasEqual) {

            ConversationGreenDbObject conversation = DbManager.getsInstance().messageToConversationDb(messageModel);

            mItems.add(0, conversation);
        }

        mAdapter.notifyDataSetChanged();
    }


    public class MessageFragmentBroadcastReceiver extends BroadcastReceiver {


        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(BroadcastConstant.clearUnreadMessage)) {

                String conversation = intent.getStringExtra("conversation");

                clearUnread(conversation);
            } else if (intent.getAction().equals(BroadcastConstant.updateConversation)) {

                queryData();
            }
            KLog.i("收到广播信息");
        }


    }

}
