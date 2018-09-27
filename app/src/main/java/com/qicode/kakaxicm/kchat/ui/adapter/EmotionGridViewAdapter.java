package com.qicode.kakaxicm.kchat.ui.adapter;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.qicode.kakaxicm.kchat.R;

import java.util.List;

/**
 * Created by chenming on 2018/9/23
 */
public class EmotionGridViewAdapter extends BaseAdapter{
    private Context mContext;
    private List<String> mEmotionNames;
    private int mItemWidth;

    public EmotionGridViewAdapter(Context context, List<String> emotionNames, int itemWidth){
        this.mContext = context;
        this.mEmotionNames = emotionNames;
        this.mItemWidth = itemWidth;
    }

    @Override
    public int getCount() {
        return mEmotionNames.size() + 1;
    }

    @Override
    public Object getItem(int i) {
        return mEmotionNames.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        //最后一个为删除键
        if (i == getCount() - 1) {
            ImageView imageView = new ImageView(mContext);
            imageView.setPadding(mItemWidth /8, mItemWidth /8, mItemWidth /8, mItemWidth /8);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(mItemWidth, mItemWidth);
            imageView.setLayoutParams(params);
            imageView.setImageResource(R.drawable.compose_emotion_delete);
            return imageView;
        } else {
            TextView textView = new TextView(mContext);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(mItemWidth, mItemWidth);
            textView.setLayoutParams(params);
            textView.setGravity(Gravity.CENTER);
            textView.setTextColor(0xff000000);
            textView.setTextSize(20);
            String emotionName = mEmotionNames.get(i);
            textView.setText(emotionName);
            return textView;
        }

    }
}
