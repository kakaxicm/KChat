package com.qicode.kakaxicm.kchat.ui.fragment;

import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.blankj.utilcode.util.ScreenUtils;
import com.qicode.kakaxicm.kchat.R;
import com.qicode.kakaxicm.kchat.ui.adapter.EmotionGridViewAdapter;
import com.qicode.kakaxicm.kchat.ui.adapter.EmotionPagerAdapter;
import com.qicode.kakaxicm.kchat.ui.view.IndicatorView;
import com.qicode.kakaxicm.kchat.util.EmotionUtil;
import com.qicode.kakaxicm.kchat.util.KUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

/**
 * Created by chenming on 2018/9/23
 */
public class ChatEmotionFragment extends BaseFragment {
    @BindView(R.id.fragment_chat_vp)
    ViewPager mFragmentChatVp;
    @BindView(R.id.fragment_chat_group)
    IndicatorView mFragmentChatGroup;
    private View rootView;
    //viewpager适配器
    private EmotionPagerAdapter mEmotionPagerAdapter;
    //每个表情的点击事件
    private EmotionClickCallBack mClickCallBack;

    private int mChatEmotionHeight;


    public int getChatEmotionHeight() {
        return mChatEmotionHeight;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_chat_emotion;
    }

    @Override
    protected void initView() {
        super.initView();
        initWidget();
    }

    public void setClickCallBack(EmotionClickCallBack mClickCallBack) {
        this.mClickCallBack = mClickCallBack;
    }

    private void initWidget() {
        mFragmentChatVp.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            int oldPagerPos = 0;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mFragmentChatGroup.playByStartPointToNext(oldPagerPos, position);
                oldPagerPos = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        initEmotion();
    }

    private void initEmotion() {

        int screenWidth = ScreenUtils.getScreenWidth();
        int spacing = KUtil.dip2px(mContext, 12);
        int itemWidth = (screenWidth - spacing * 8) / 7;
        int gvHeight = itemWidth * 3 + spacing * 6;

        mChatEmotionHeight = gvHeight + KUtil.dip2px(mContext, 30);

        List<GridView> emotionViews = new ArrayList<>();
        List<String> emotionNames = new ArrayList<>();
        for (String emotion : EmotionUtil.emotions) {
            emotionNames.add(emotion);
            if (emotionNames.size() == 23) {
                GridView gv = createEmotionGridView(emotionNames, screenWidth, spacing, itemWidth, gvHeight);
                emotionViews.add(gv);
                emotionNames = new ArrayList<>();
            }
        }

        if (emotionNames.size() > 0) {
            GridView gv = createEmotionGridView(emotionNames, screenWidth, spacing, itemWidth, gvHeight);
            emotionViews.add(gv);
        }

        mFragmentChatGroup.initIndicator(emotionViews.size());

        mEmotionPagerAdapter = new EmotionPagerAdapter(emotionViews);
        mFragmentChatVp.setAdapter(mEmotionPagerAdapter);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(screenWidth, gvHeight);
        mFragmentChatVp.setLayoutParams(params);
    }

    private GridView createEmotionGridView(final List<String> emotionNames, int gvWidth, int padding, int itemWidth, int gvHeight) {

        GridView gv = new GridView(mContext);
        gv.setNumColumns(8);
        gv.setPadding(padding, padding, padding, padding);
        gv.setHorizontalSpacing(padding);
        gv.setVerticalSpacing(padding * 2);

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(gvWidth, gvHeight);
        gv.setLayoutParams(params);

        EmotionGridViewAdapter adapter = new EmotionGridViewAdapter(mContext, emotionNames, itemWidth);
        gv.setAdapter(adapter);

        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                if (mClickCallBack == null) {
                    return;
                }
                if (view instanceof ImageView) {
                    mClickCallBack.onClickDelete();
                } else {
                    String emotion = emotionNames.get(i);
                    mClickCallBack.onClickEmotion(emotion);
                }


            }
        });

        return gv;
    }


    public interface EmotionClickCallBack {

        void onClickEmotion(String emotion);

        void onClickDelete();
    }
}
