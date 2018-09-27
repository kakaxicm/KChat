package com.qicode.kakaxicm.kchat.ui.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.qicode.kakaxicm.kchat.R;
import com.qicode.kakaxicm.kchat.util.KUtil;

import java.util.ArrayList;

/**
 * Created by chenming on 2018/9/23
 */
public class IndicatorView extends LinearLayout {
    private Context mContext;
    private ArrayList<View> mImageViews;
    private int size = 6;
    private int marginSize = 15;
    private int pointSize;
    private int marginLeft;

    public IndicatorView(Context context) {
        this(context, null);
    }

    public IndicatorView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IndicatorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        pointSize = KUtil.dip2px(context, size);
        marginLeft = KUtil.dip2px(context, marginSize);
    }

    public void initIndicator(int count) {
        mImageViews = new ArrayList<>();
        this.removeAllViews();
        LayoutParams lp;

        for (int i = 0; i < count; i++) {
            View v = new View(mContext);
            lp = new LayoutParams(pointSize, pointSize);
            if (i != 0) {
                lp.leftMargin = marginLeft;
            }
            v.setLayoutParams(lp);
            if (i == 0){
                v.setBackgroundResource(R.drawable.bg_circle_white);
            } else {
                v.setBackgroundResource(R.drawable.bg_circle_gray);
            }
            mImageViews.add(v);
            this.addView(v);
        }
    }

    public void playByStartPointToNext(int startPosition, int nextPosition) {
        if (startPosition < 0 || nextPosition < 0 || nextPosition == startPosition) {
            startPosition = nextPosition = 0;
        }
        final View viewStart = mImageViews.get(startPosition);
        final View viewNext = mImageViews.get(nextPosition);
        viewNext.setBackgroundResource(R.drawable.bg_circle_white);
        viewStart.setBackgroundResource(R.drawable.bg_circle_gray);
    }
}
