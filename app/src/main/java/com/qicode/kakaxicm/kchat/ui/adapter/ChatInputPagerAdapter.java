package com.qicode.kakaxicm.kchat.ui.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;

/**
 * Created by chenming on 2018/9/23
 */
public class ChatInputPagerAdapter extends FragmentPagerAdapter {
    ArrayList<Fragment> mList;

    public ChatInputPagerAdapter(FragmentManager fm, ArrayList<Fragment> list) {
        super(fm);
        this.mList = list;
    }

    @Override
    public Fragment getItem(int position) {
        return mList.get(position);
    }

    @Override
    public int getCount() {
        return mList != null ? mList.size() : 0;
    }
}
