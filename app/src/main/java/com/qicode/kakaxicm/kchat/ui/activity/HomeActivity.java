package com.qicode.kakaxicm.kchat.ui.activity;

import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.widget.ImageView;
import android.widget.TextView;

import com.qicode.kakaxicm.kchat.R;
import com.qicode.kakaxicm.kchat.ui.adapter.HomePagerAdapter;
import com.qicode.kakaxicm.kchat.ui.fragment.HomeContactFragment;
import com.qicode.kakaxicm.kchat.ui.fragment.HomeMeFragment;
import com.qicode.kakaxicm.kchat.ui.fragment.HomeMessageFragment;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by chenming on 2018/9/22
 */
public class HomeActivity extends BaseActivity {
    @BindView(R.id.home_view_pager)
    ViewPager viewPager;

    @BindView(R.id.tab_message_image)
    ImageView messageImage;

    @BindView(R.id.tab_message_text)
    TextView messageText;

    @BindView(R.id.tab_contact_image)
    ImageView contactImage;

    @BindView(R.id.tab_contact_text)
    TextView contactText;

    @BindView(R.id.tab_me_image)
    ImageView meImage;

    @BindView(R.id.tab_me_text)
    TextView meText;

    @OnClick(R.id.llmessage)
    void messageClick(){

        tabItemClick(0);
    }

    @OnClick(R.id.llcontact)
    void  contactClick(){

        tabItemClick(1);
    }

    @OnClick(R.id.llme)
    void meClick(){
        tabItemClick(2);
    }


    List<Fragment> mFragments = new ArrayList<>();
    List<ImageView> mImageViews = new ArrayList<>();
    List<TextView> mTextViews = new ArrayList<>();

    @Override
    protected int getLayoutId() {
        return R.layout.activity_home;
    }

    @Override
    protected void initView() {
        super.initView();


        setTitle("FoxChat");
        hasBack(false);

        mImageViews.add(messageImage);
        mImageViews.add(contactImage);
        mImageViews.add(meImage);

        mTextViews.add(messageText);
        mTextViews.add(contactText);
        mTextViews.add(meText);

        mFragments.add(new HomeMessageFragment());
        mFragments.add(new HomeContactFragment());
        mFragments.add(new HomeMeFragment());

        tabItemClick(0);

        HomePagerAdapter adapter = new HomePagerAdapter(getSupportFragmentManager(), mFragments);
        viewPager.setOffscreenPageLimit(2);
        viewPager.setAdapter(adapter);


        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {


                viewPagerSelected(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void viewPagerSelected(int position){

        tabItemClick(position);

    }

    private void tabItemClick(int position) {

        String [] titles = {"FoxChat", "联系人", "我"};
        setTitle(titles[position]);
        viewPager.setCurrentItem(position, false);
        for(ImageView imageView : mImageViews) {

            imageView.setSelected(false);
        }
        for (TextView textView : mTextViews) {
            textView.setSelected(false);
        }

        ImageView seletedImage = mImageViews.get(position);
        seletedImage.setSelected(true);

        TextView seletedTextView = mTextViews.get(position);
        seletedTextView.setSelected(true);
    }
}
