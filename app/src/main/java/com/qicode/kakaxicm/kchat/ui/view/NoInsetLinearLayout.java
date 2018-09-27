package com.qicode.kakaxicm.kchat.ui.view;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.WindowInsets;
import android.widget.LinearLayout;

/**
 * Created by chenming on 2018/9/25
 */
public class NoInsetLinearLayout extends LinearLayout {
    private int[] mInsets = new int[4];
    public NoInsetLinearLayout(Context context) {
        super(context);
    }

    public NoInsetLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public NoInsetLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public NoInsetLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected boolean fitSystemWindows(Rect insets) {
        return super.fitSystemWindows(insets);
    }


    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {

            mInsets[0] = insets.getSystemWindowInsetLeft();
            mInsets[1] = insets.getSystemWindowInsetTop();
            mInsets[2] = insets.getSystemWindowInsetRight();

            return super.onApplyWindowInsets(insets.replaceSystemWindowInsets(0, 0, 0, insets.getSystemWindowInsetBottom()));
        }
        else {
            return super.onApplyWindowInsets(insets);
        }
    }
}
