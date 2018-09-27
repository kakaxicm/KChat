package com.qicode.kakaxicm.kchat.util;

import android.media.MediaPlayer;

import java.io.IOException;

/**
 * Created by chenming on 2018/9/13
 * 播放录音的工具类
 */
public class AudioPlayUtil {
    /**
     * 播放网络音频
     * @param url
     */
    public static void playNetAudio(String url) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(url);
            //3 准备播放
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.start();
                }
            });


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
