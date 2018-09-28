package com.qicode.kakaxicm.kchat.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blankj.utilcode.util.KeyboardUtils;
import com.blankj.utilcode.util.ScreenUtils;
import com.blankj.utilcode.util.SizeUtils;
import com.bumptech.glide.Glide;
import com.lzy.imagepicker.ImagePicker;
import com.lzy.imagepicker.bean.ImageItem;
import com.lzy.imagepicker.ui.ImageGridActivity;
import com.qicode.kakaxicm.kchat.R;
import com.qicode.kakaxicm.kchat.constant.BroadcastConstant;
import com.qicode.kakaxicm.kchat.constant.UrlConstant;
import com.qicode.kakaxicm.kchat.db.DbManager;
import com.qicode.kakaxicm.kchat.enums.MessageBtnType;
import com.qicode.kakaxicm.kchat.manager.ChatManager;
import com.qicode.kakaxicm.kchat.manager.ClientManager;
import com.qicode.kakaxicm.kchat.model.MessageEvent;
import com.qicode.kakaxicm.kchat.model.MessageModel;
import com.qicode.kakaxicm.kchat.ui.MsgImageLoader;
import com.qicode.kakaxicm.kchat.ui.adapter.ChatInputPagerAdapter;
import com.qicode.kakaxicm.kchat.ui.fragment.ChatEmotionFragment;
import com.qicode.kakaxicm.kchat.ui.fragment.ChatInputOtherFragment;
import com.qicode.kakaxicm.kchat.ui.view.AudioRecordPopupWindow;
import com.qicode.kakaxicm.kchat.ui.view.NoScrollViewPager;
import com.qicode.kakaxicm.kchat.util.AudioPlayUtil;
import com.qicode.kakaxicm.kchat.util.AudioRecordUtil;
import com.qicode.kakaxicm.kchat.util.KUtil;
import com.qicode.kakaxicm.kchat.util.SoftKeyBoardListener;
import com.zhy.adapter.abslistview.MultiItemTypeAdapter;
import com.zhy.adapter.abslistview.ViewHolder;
import com.zhy.adapter.abslistview.base.ItemViewDelegate;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by chenming on 2018/9/23
 */
public class ChatActivity extends BaseActivity {

    private int mPage = 0;
    private int mLimit = 5;

    private MultiItemTypeAdapter mAdapter;
    List<MessageModel> mMessageList = new ArrayList<MessageModel>();
    private ChatEmotionFragment mChatEmotionFragment;
    //按钮类型默认发送文本
    private MessageBtnType mBtnType = MessageBtnType.MsgBtnText;
    //发送语音的pop
    private AudioRecordPopupWindow mVoicePop;
    //其他输入类型VP适配器
    private ChatInputPagerAdapter mViewpagerAdapter;
    //录音工具
    private AudioRecordUtil mAudioRecordUtil;
    //聊天对方的username
    private String mFriendsUserName;

    @BindView(R.id.swipe_layout)
    SwipeRefreshLayout refreshLayout;

    @BindView(R.id.chat_list_view)
    ListView listView;

    @BindView(R.id.chat_voice)
    ImageView voiceBtn;

    @BindView(R.id.chat_emotion)
    ImageView emotionBtn;

    @BindView(R.id.chat_add_other)
    ImageView addOtherBtn;

    @BindView(R.id.chat_input_text)
    EditText inputTextMessage;


    @BindView(R.id.chat_send_voice)
    TextView sendVoiceBtn;

    @BindView(R.id.chat_text_send_btn)
    Button sendMessageBtn;

    @BindView(R.id.chat_msg_input_other_back)
    RelativeLayout inputBackLayout;

    @BindView(R.id.chat_msg_input_viewpager)
    NoScrollViewPager viewPager;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_chat;
    }


    @Override
    protected void initView() {
        //键盘show/hide监听
        SoftKeyBoardListener.setListener(this, new SoftKeyBoardListener.OnSoftKeyBoardChangeListener() {
            @Override
            public void keyBoardShow(int height) {

                setmBtnType(MessageBtnType.MsgBtnText);
            }

            @Override
            public void keyBoardHide(int height) {

            }
        });

        //获取intent数据
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String userName = bundle.getString("username");
            setTitle(userName);
            ClientManager.chattingUserId = userName;
            mFriendsUserName = userName;
        }

        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshMessageList(false);
            }
        });
        //消息列表
        mAdapter = new MultiItemTypeAdapter(this, mMessageList);
        mAdapter.addItemViewDelegate(new MsgLeftTextItemDelegate());
        mAdapter.addItemViewDelegate(new MsgRightTextItemDelegate());
        listView.setAdapter(mAdapter);

        //接收Socket层发来的消息
        EventBus.getDefault().register(this);
        //文本输入框监听
        inputTextMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                if (charSequence.length() > 0) {
                    addOtherBtn.setVisibility(View.GONE);
                    sendMessageBtn.setVisibility(View.VISIBLE);
                } else {
                    addOtherBtn.setVisibility(View.VISIBLE);
                    sendMessageBtn.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        initVoicePop();

        refreshMessageList(true);
        initEmojiOtherUI();
    }

    /**
     * 初始化表情fragment
     */
    private void initEmojiOtherUI() {
        // viewpager
        ArrayList<Fragment> fragments = new ArrayList<>();
        mChatEmotionFragment = new ChatEmotionFragment();
        mChatEmotionFragment.setClickCallBack(new ChatEmotionFragment.EmotionClickCallBack() {
            @Override
            public void onClickEmotion(String emotion) {
                int curPosition = inputTextMessage.getSelectionStart();
                StringBuilder sb = new StringBuilder(inputTextMessage.getText().toString());
                sb.insert(curPosition, emotion);
                inputTextMessage.setText(sb.toString());
                inputTextMessage.setSelection(curPosition + emotion.length());
            }

            @Override
            public void onClickDelete() {

                inputTextMessage.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
            }
        });
        ChatInputOtherFragment otherFragment = new ChatInputOtherFragment();
        otherFragment.setClickCallBack(new ChatInputOtherFragment.AddItemClickCallBack() {
            @Override
            public void clickItemIndex(int index) {

                setmBtnType(MessageBtnType.MsgBtnText);
                KeyboardUtils.hideSoftInput(inputTextMessage);
                switch (index) {
                    case 0:
                        pickImage();
                        break;
                    case 1:
                        takeCamera();
                        break;
                    case 2:
                        pickLocation();
                        break;
                }
            }
        });
        fragments.add(mChatEmotionFragment);
        fragments.add(otherFragment);
        mViewpagerAdapter = new ChatInputPagerAdapter(getSupportFragmentManager(), fragments);
        viewPager.setAdapter(mViewpagerAdapter);
        viewPager.setCurrentItem(0);
    }

    /**
     * 拍摄图片
     */
    private void takeCamera() {

        Intent intent = new Intent(this, CameraActivity.class);
        startActivityForResult(intent, 1001);
    }


    /**
     * 选择图片
     */
    private void pickImage() {

        ImagePicker imagePicker = ImagePicker.getInstance();
        imagePicker.setImageLoader(new MsgImageLoader());
        imagePicker.setShowCamera(true);
        imagePicker.setCrop(true);
        imagePicker.setSelectLimit(3);

        Intent intent = new Intent(this, ImageGridActivity.class);
        startActivityForResult(intent, 100);
    }

    /*
     * 选择位置
     * */
    private void pickLocation() {
        Intent intent = new Intent(this, LocationActivity.class);
        startActivityForResult(intent, 1000);
    }

    //emoji按钮点击
    @OnClick(R.id.chat_emotion)
    void emotionClick() {

        if (mBtnType != MessageBtnType.MsgBtnEmotion) {

            setmBtnType(MessageBtnType.MsgBtnEmotion);
        } else {

            setmBtnType(MessageBtnType.MsgBtnText);
        }
    }

    //其他方式点击
    @OnClick(R.id.chat_add_other)
    void addOtherClick() {

        if (mBtnType != MessageBtnType.MsgBtnOther) {

            setmBtnType(MessageBtnType.MsgBtnOther);

        } else {

            setmBtnType(MessageBtnType.MsgBtnText);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initVoicePop() {
        View view = View.inflate(mContext, R.layout.layout_microphone, null);
        mVoicePop = new AudioRecordPopupWindow(this, view);

        final ImageView voiceImageView = view.findViewById(R.id.iv_recording_icon);
        final TextView voiceTimeView = view.findViewById(R.id.tv_recording_time);
        final TextView voiceTextView = view.findViewById(R.id.tv_recording_text);

        sendVoiceBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                // 获取x坐标
                int x = (int) motionEvent.getX();
                // 获取y坐标
                int y = (int) motionEvent.getY();

                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mVoicePop.showAtLocation(view, Gravity.CENTER, 0, 0);
                        sendVoiceBtn.setText("松开结束");
                        voiceTextView.setText("手指上滑，取消发送");
                        sendVoiceBtn.setTag("1");
                        mAudioRecordUtil.startRecord(mContext);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (wantToCancel(x, y)) {
                            sendVoiceBtn.setText("松开结束");
                            voiceTextView.setText("松开手指，取消发送");
                            sendVoiceBtn.setTag("2");
                        } else {
                            sendVoiceBtn.setText("松开结束");
                            voiceTextView.setText("手指上滑，取消发送");
                            sendVoiceBtn.setTag("1");
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        mVoicePop.dismiss();
                        if (sendVoiceBtn.getTag().equals("2")) {

                            mAudioRecordUtil.cancelRecord();
                        } else {

                            mAudioRecordUtil.stopRecord();
                        }
                        sendVoiceBtn.setText("按住说话");
                        sendVoiceBtn.setTag("3");
                        break;
                }

                return true;
            }
        });

        mAudioRecordUtil = new AudioRecordUtil();

        mAudioRecordUtil.setOnAudioStatusUpdateListener(new AudioRecordUtil.OnAudioStatusUpdateListener() {
            @Override
            public void onUpdate(double db, long time) {

                voiceImageView.getDrawable().setLevel((int) (3000 + 6000 * db / 100));
                voiceTimeView.setText(KUtil.long2String(time));
            }

            @Override
            public void onStop(long time, String filePath, String audioName) {

                voiceTimeView.setText(KUtil.long2String(0));
                if (time < 1) {
                    KUtil.showShortToast(mContext, "录音时间过短");
                } else {

                    //发送语音消息
                    sendAudioMsg(audioName, time);
                }
            }

            @Override
            public void onError() {

            }
        });
    }

    @OnClick(R.id.chat_text_send_btn)
    void sendTextMsg() {

        if (inputTextMessage.getText().toString().trim().length() == 0) {
            return;
        }
        MessageModel messageModel = ChatManager.sendTextMsg(inputTextMessage.getText().toString(), mFriendsUserName, new ChatManager.SendStatusCallBack() {
            @Override
            public void sendStatus(MessageModel messageModel) {
                sendMsgSuccess();
            }
        });
        inputTextMessage.setText("");

        // 刷新UI，一旦发送消息，不管是否发送成功，则更新列表
        sendMsgAfter(messageModel);
    }

    /*
     * 发送音频
     * */
    private void sendAudioMsg(String audioName, long duration) {

        MessageModel messageModel = ChatManager.sendAudioMsg(audioName, duration, mFriendsUserName, new ChatManager.SendStatusCallBack() {
            @Override
            public void sendStatus(MessageModel messageModel) {
                //发送成功后再刷新一次列表
                sendMsgSuccess();
            }
        });

        sendMsgAfter(messageModel);
    }

    /**
     * 发送图片
     *
     * @param imagePath
     * @param imageName
     * @param width
     * @param height
     */
    private void sendImage(String imagePath, String imageName, int width, int height) {

        HashMap size = new HashMap();
        size.put("width", width);
        size.put("height", height);
        MessageModel messageModel = ChatManager.sendImageMsg(imagePath, imageName, size, mFriendsUserName, new ChatManager.SendStatusCallBack() {
            @Override
            public void sendStatus(MessageModel messageModel) {

                sendMsgSuccess();
            }
        });

        sendMsgAfter(messageModel);
    }


    /**
     * 发送消息后，更新消息列表
     *
     * @param messageModel
     */
    private void sendMsgAfter(MessageModel messageModel) {
        mMessageList.add(messageModel);
        mAdapter.notifyDataSetChanged();
        listView.smoothScrollToPosition(mMessageList.size() - 1);

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        localBroadcastManager.sendBroadcast(new Intent(BroadcastConstant.updateConversation));
    }

    /**
     * 消息发送成功,再刷新一下消息列表
     */
    private void sendMsgSuccess() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    private boolean wantToCancel(int x, int y) {
        if (x < 0 || x > sendVoiceBtn.getWidth()) {
            return true;
        }
        if (y < -50 || y > sendVoiceBtn.getHeight() + 50) {
            return true;
        }
        return false;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventBus(MessageEvent event) {

        switch (event.getType()) {
            case EventMessage: // 收到新消息


                MessageModel message = (MessageModel) event.getMsg();

                if (!message.getFrom_user().equals(mFriendsUserName) && !message.getTo_user().equals(mFriendsUserName)) { // 不属于该会话的消息
                    return;
                }
                mMessageList.add(message);
                mAdapter.notifyDataSetChanged();
                listView.smoothScrollToPosition(mMessageList.size() - 1);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == ImagePicker.RESULT_CODE_ITEMS) {
            if (data != null && requestCode == 100) {

                ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
                for (ImageItem imageItem : images) {

                    HashMap size = new HashMap();
                    sendImage(imageItem.path, null, imageItem.width, imageItem.height);
                }
            }
        } else if (resultCode == 101) { // 发送定位的回调

            double lat = data.getDoubleExtra("lat", 0.0);
            double lon = data.getDoubleExtra("lon", 0.0);
            String location = data.getStringExtra("location");
            String detail = data.getStringExtra("detailLocation");
            sendLocationMsg(lat, lon, location, detail);
        } else if (resultCode == 102) { // 拍摄回调

            String imageName = data.getStringExtra("imageName");
            int imageWidth = data.getIntExtra("imageWidth", 0);
            int imageHeight = data.getIntExtra("imageHeight", 0);

            sendImage(null, imageName, imageWidth, imageHeight);
        }
    }

    private void sendLocationMsg(double lat, double lon, String location, String detail) {

        MessageModel messageModel = ChatManager.sendLocationMsg(lat, lon, location, detail, mFriendsUserName, new ChatManager.SendStatusCallBack() {
            @Override
            public void sendStatus(MessageModel messageModel) {

                sendMsgSuccess();
            }
        });
        sendMsgAfter(messageModel);
    }


    //上拉加载更多
    private void refreshMessageList(final boolean scrollToBottom) {
        //查询本地消息列表
        DbManager.getsInstance().queryMessages(mFriendsUserName, mPage, mLimit, new DbManager.QueryDbCallBack<MessageModel>() {
            @Override
            public void querySuccess(List<MessageModel> items, boolean hasMore) {

                refreshLayout.setRefreshing(false);
                mPage++;

                if (items.size() == 0) {
                    refreshLayout.setEnabled(false);
                    KUtil.showShortToast(mContext, "没有更多消息");
                } else {

                    if (!hasMore) {
                        refreshLayout.setEnabled(false);
                    }

                    mMessageList.addAll(0, items);
                    mAdapter.notifyDataSetChanged();

                    if (scrollToBottom) {
                        listView.smoothScrollToPosition(mMessageList.size() - 1);
                    }
                }
            }
        });
    }

    /**
     * 设置按钮状态
     *
     * @param mBtnType
     */
    private void setmBtnType(MessageBtnType mBtnType) {
        this.mBtnType = mBtnType;

        voiceBtn.setImageResource(R.drawable.keyboard_voice);
        emotionBtn.setImageResource(R.drawable.keyboard_emotion);
        addOtherBtn.setImageResource(R.drawable.keyboard_add);

        if (mBtnType == MessageBtnType.MsgBtnVoice) {

            inputTextMessage.setVisibility(View.GONE);
            sendVoiceBtn.setVisibility(View.VISIBLE);
        } else {
            inputTextMessage.setVisibility(View.VISIBLE);
            sendVoiceBtn.setVisibility(View.GONE);
        }

        switch (mBtnType) {

            case MsgBtnVoice:
                voiceBtn.setImageResource(R.drawable.keyboard_keyboard);
                showEmotionOrOther(mBtnType, false);
                KeyboardUtils.hideSoftInput(this);
                break;
            case MsgBtnEmotion:
                emotionBtn.setImageResource(R.drawable.keyboard_keyboard);
                showEmotionOrOther(mBtnType, true);
                break;
            case MsgBtnOther:
                addOtherBtn.setImageResource(R.drawable.keyboard_keyboard);
                showEmotionOrOther(mBtnType, true);
                break;
            case MsgBtnText:
                showEmotionOrOther(mBtnType, false);
                break;
        }
    }

    /**
     * 显示表情或者其他方式列表
     *
     * @param type
     * @param show
     */
    private void showEmotionOrOther(MessageBtnType type, boolean show) {

        if (show) {
            KeyboardUtils.hideSoftInput(this);
            inputBackLayout.setVisibility(View.VISIBLE);

            LinearLayout.LayoutParams par = (LinearLayout.LayoutParams) inputBackLayout.getLayoutParams();
            switch (type) {
                case MsgBtnEmotion:
                    int height = mChatEmotionFragment.getChatEmotionHeight();
                    par.height = mChatEmotionFragment.getChatEmotionHeight();
                    viewPager.setCurrentItem(0);
                    break;
                case MsgBtnOther:
                    par.height = KUtil.dip2px(mContext, 150);
                    viewPager.setCurrentItem(1);
                    break;
            }
            inputBackLayout.setLayoutParams(par);
        } else {

            inputBackLayout.setVisibility(View.GONE);
        }
    }

    public class MsgLeftTextItemDelegate implements ItemViewDelegate<MessageModel> {

        @Override
        public int getItemViewLayoutId() {
            return R.layout.left_chat_item_text;
        }

        @Override
        public boolean isForViewType(MessageModel item, int position) {
            return (item.getTo_user().equals(ClientManager.currentUserId));
        }

        @Override
        public void convert(ViewHolder holder, final MessageModel messageModel, int position) {

            TextView msgTextView = holder.getView(R.id.message_text);
            ImageView imageView = holder.getView(R.id.message_img);
            ViewGroup voiceBack = holder.getView(R.id.message_voice);
            ViewGroup locationBack = holder.getView(R.id.message_location_back);
            switch (messageModel.getBodies().getType()) {
                case txt:
                    msgTextView.setVisibility(View.VISIBLE);
                    imageView.setVisibility(View.GONE);
                    voiceBack.setVisibility(View.GONE);
                    locationBack.setVisibility(View.GONE);
                    holder.setText(R.id.message_text, messageModel.getBodies().getMsg());
                    break;
                case img:
                    msgTextView.setVisibility(View.GONE);
                    imageView.setVisibility(View.VISIBLE);
                    voiceBack.setVisibility(View.GONE);
                    locationBack.setVisibility(View.GONE);
                    String imagePath = KUtil.imageSavePath() + messageModel.getBodies().getFileName();

                    Glide.with(mContext).load(imagePath).into(imageView);

                    int screenWidth = ScreenUtils.getScreenWidth();

                    int imageWidth = messageModel.getBodies().getSize().get("width").intValue();
                    int imageHeight = messageModel.getBodies().getSize().get("height").intValue();

                    imageWidth = SizeUtils.dp2px(imageWidth);
                    imageHeight = SizeUtils.dp2px(imageHeight);
                    float scale = (float) imageWidth / imageHeight;
                    if (imageWidth > screenWidth / 2) {
                        imageWidth = screenWidth / 2;
                        imageHeight = (int) (imageWidth / scale);
                    }

                    ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
                    layoutParams.width = imageWidth;
                    layoutParams.height = imageHeight;
                    break;
                case audio:
                    voiceBack.setVisibility(View.VISIBLE);
                    msgTextView.setVisibility(View.GONE);
                    imageView.setVisibility(View.GONE);
                    locationBack.setVisibility(View.GONE);
                    int minWidth = SizeUtils.dp2px(90);
                    int maxWidth = ScreenUtils.getScreenWidth() * 2 / 3;

                    long duration = messageModel.getBodies().getDuration();
                    long maxDuration = 60;
                    if (duration > maxDuration) {
                        duration = maxDuration;
                    }
                    int width = (int) (minWidth + (float) (maxWidth - minWidth) / maxDuration * duration);
                    voiceBack.getLayoutParams().width = width;
                    holder.setText(R.id.message_voice_duration, KUtil.long2String(duration));

                    holder.getConvertView().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //取远程录音文件
                            final String path = UrlConstant.baseUrl + "/" + messageModel.getBodies().getFileRemotePath();
                            //播放录音文件
                            AudioPlayUtil.playNetAudio(path);
                        }
                    });
                    break;
                case loc:
                    msgTextView.setVisibility(View.GONE);
                    imageView.setVisibility(View.GONE);
                    voiceBack.setVisibility(View.GONE);
                    locationBack.setVisibility(View.VISIBLE);

                    holder.setText(R.id.message_location_name, messageModel.getBodies().getLocationName());
                    holder.setText(R.id.message_location_detail, messageModel.getBodies().getDetailLocationName());

                    ImageView locationImage = holder.getView(R.id.message_location_img);
                    String path = UrlConstant.baseUrl + "/" + messageModel.getBodies().getFileRemotePath();
                    Glide.with(mContext).load(path).into(locationImage);

                    break;
            }


        }

    }

    public class MsgRightTextItemDelegate implements ItemViewDelegate<MessageModel> {

        @Override
        public int getItemViewLayoutId() {
            return R.layout.right_chat_item_text;
        }

        @Override
        public boolean isForViewType(MessageModel item, int position) {
            return item.getFrom_user().equals(ClientManager.currentUserId);
        }

        @Override
        public void convert(ViewHolder holder, final MessageModel messageModel, int position) {

            holder.setText(R.id.message_text, messageModel.getBodies().getMsg());

            ProgressBar bar = holder.getView(R.id.msg_send_progress);
            ImageView failIcon = holder.getView(R.id.msg_send_fail);
            ViewGroup voiceBack = holder.getView(R.id.message_voice);
            ViewGroup locationBack = holder.getView(R.id.message_location_back);
            switch (messageModel.getSendStatus()) {
                case MessageSendSuccess:
                    bar.setVisibility(View.GONE);
                    failIcon.setVisibility(View.GONE);
                    break;

                case MessageSending:
                    bar.setVisibility(View.VISIBLE);
                    failIcon.setVisibility(View.GONE);
                    break;

                case MessageSendFail:
                    bar.setVisibility(View.GONE);
                    bar.setVisibility(View.VISIBLE);
                    break;
            }


            TextView msgTextView = holder.getView(R.id.message_text);
            ImageView imageView = holder.getView(R.id.message_img);

            switch (messageModel.getBodies().getType()) {
                case txt:
                    msgTextView.setVisibility(View.VISIBLE);
                    imageView.setVisibility(View.GONE);
                    voiceBack.setVisibility(View.GONE);
                    locationBack.setVisibility(View.GONE);
                    holder.setText(R.id.message_text, messageModel.getBodies().getMsg());
                    break;
                case img:
                    msgTextView.setVisibility(View.GONE);
                    imageView.setVisibility(View.VISIBLE);
                    voiceBack.setVisibility(View.GONE);
                    locationBack.setVisibility(View.GONE);
                    String imagePath = KUtil.imageSavePath() + messageModel.getBodies().getFileName();

                    Glide.with(mContext).load(imagePath).into(imageView);

                    int screenWidth = ScreenUtils.getScreenWidth();

                    int imageWidth = messageModel.getBodies().getSize().get("width").intValue();
                    int imageHeight = messageModel.getBodies().getSize().get("height").intValue();

                    imageWidth = SizeUtils.dp2px(imageWidth);
                    imageHeight = SizeUtils.dp2px(imageHeight);
                    float scale = (float) imageWidth / imageHeight;
                    if (imageWidth > screenWidth / 2) {
                        imageWidth = screenWidth / 2;
                        imageHeight = (int) (imageWidth / scale);
                    }

                    ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
                    layoutParams.width = imageWidth;
                    layoutParams.height = imageHeight;
                    break;
                case audio:
                    msgTextView.setVisibility(View.GONE);
                    imageView.setVisibility(View.GONE);
                    voiceBack.setVisibility(View.VISIBLE);
                    locationBack.setVisibility(View.GONE);
                    int minWidth = SizeUtils.dp2px(90);
                    int maxWidth = ScreenUtils.getScreenWidth() * 2 / 3;

                    long duration = messageModel.getBodies().getDuration();
                    long maxDuration = 60;
                    if (duration > maxDuration) {
                        duration = maxDuration;
                    }
                    int width = (int) (minWidth + (float) (maxWidth - minWidth) / maxDuration * duration);
                    voiceBack.getLayoutParams().width = width;
                    holder.setText(R.id.message_voice_duration, KUtil.long2String(duration));

                    holder.getConvertView().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //取远程录音文件
                            final String path = UrlConstant.baseUrl + "/" + messageModel.getBodies().getFileRemotePath();
                            //播放录音文件
                            AudioPlayUtil.playNetAudio(path);
                        }
                    });
                    break;
                case loc:
                    msgTextView.setVisibility(View.GONE);
                    imageView.setVisibility(View.GONE);
                    voiceBack.setVisibility(View.GONE);
                    locationBack.setVisibility(View.VISIBLE);

                    holder.setText(R.id.message_location_name, messageModel.getBodies().getLocationName());
                    holder.setText(R.id.message_location_detail, messageModel.getBodies().getDetailLocationName());

                    ImageView locationImage = holder.getView(R.id.message_location_img);
                    String path = UrlConstant.baseUrl + "/" + messageModel.getBodies().getFileRemotePath();
                    Glide.with(mContext).load(path).into(locationImage);

                    break;
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ClientManager.chattingUserId = "";
    }
}
