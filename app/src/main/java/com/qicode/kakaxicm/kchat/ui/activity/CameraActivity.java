package com.qicode.kakaxicm.kchat.ui.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.view.View;

import com.cjt2325.cameralibrary.JCameraView;
import com.cjt2325.cameralibrary.listener.ErrorListener;
import com.cjt2325.cameralibrary.listener.JCameraListener;
import com.qicode.kakaxicm.kchat.R;
import com.qicode.kakaxicm.kchat.util.KLog;
import com.qicode.kakaxicm.kchat.util.KUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by chenming on 2018/9/25
 */
public class CameraActivity extends BaseActivity {
    private JCameraView jCameraView;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_camera;
    }

    @Override
    protected void initView() {
        if (Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(option);
        }

        jCameraView = findViewById(R.id.jcameraview);
        jCameraView.setSaveVideoPath(KUtil.videoSavePath());
        jCameraView.setMediaQuality(JCameraView.MEDIA_QUALITY_MIDDLE);

        jCameraView.setErrorLisenter(new ErrorListener() {
            @Override
            public void onError() {

                KLog.i("打开camera失败");
            }

            @Override
            public void AudioPermissionError() {

                KLog.i("没有权限");
            }
        });

        jCameraView.setJCameraLisenter(new JCameraListener() {
            @Override
            public void captureSuccess(Bitmap bitmap) {

                sendImage(bitmap);
            }

            @Override
            public void recordSuccess(String url, Bitmap firstFrame) {
                //TODO 发送视频处理
                KLog.i("获取到视频");
            }
        });

    }

    private void sendImage(Bitmap bitmap) {

        // 保存图片到本地
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] buffer = stream.toByteArray();

        String fileName = KUtil.createUUID() + ".jpg";

        String savePath = KUtil.imageSavePath() + fileName;

        File file = new File(savePath);
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(buffer);
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Intent intent = new Intent();
        intent.putExtra("imageName", fileName);
        intent.putExtra("imageWidth", bitmap.getWidth());
        intent.putExtra("imageHeight", bitmap.getHeight());
        setResult(102, intent);
        goBack();
    }
}
