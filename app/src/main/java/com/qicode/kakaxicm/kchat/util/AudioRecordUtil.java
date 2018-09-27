package com.qicode.kakaxicm.kchat.util;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Handler;

import java.io.File;
import java.io.IOException;

/**
 * Created by chenming on 2018/9/23
 * 录音功能封装
 */
public class AudioRecordUtil {

    private int BASE = 1;
    private int SPACE = 100;

    // 文件路径
    private String filePath;
    // 文件夹路径
    private String folderPath;
    // 文件名称
    private String audioName;
    //录音类
    private MediaRecorder mMediaRecorder;
    //最长时间ms
    private static final int MAX_LENGTH = 1000 * 60 * 10;
    //录音过程监听
    private OnAudioStatusUpdateListener audioStatusUpdateListener;

    private long startTime;
    private long endTime;

    public AudioRecordUtil() {
        this(KUtil.audioSavePath());
    }

    public AudioRecordUtil(String filePath) {
        this.folderPath = filePath;
    }

    public void startRecord(Context context) {
        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        }
        try {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            audioName = KUtil.createUUID() + ".amr";
            filePath = folderPath + audioName;

            mMediaRecorder.setOutputFile(filePath);
            mMediaRecorder.setMaxDuration(MAX_LENGTH);
            mMediaRecorder.prepare();

            mMediaRecorder.start();

            startTime = System.currentTimeMillis();
            //更新状态
            updateMicStatus();
        } catch (IllegalStateException e) {

            audioStatusUpdateListener.onError();
        } catch (IOException e) {

            audioStatusUpdateListener.onError();
        }

    }

    /**
     * 停止录音
     * @return
     */
    public long stopRecord() {

        if (mMediaRecorder == null){
            return 0L;
        }
        endTime = System.currentTimeMillis();

        mMediaRecorder.setOnErrorListener(null);
        mMediaRecorder.setPreviewDisplay(null);

        mMediaRecorder.stop();
        mMediaRecorder.reset();
        mMediaRecorder.release();
        mMediaRecorder = null;

        long time = endTime - startTime;
        audioStatusUpdateListener.onStop(time/1000, filePath, audioName);
        filePath = "";
        return  endTime - startTime;
    }

    /**
     * 取消录音
     */
    public void cancelRecord() {
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        File file = new File(filePath);
        if (file.exists())
            file.delete();
        filePath = null;
    }

    private final Handler mHandler = new Handler();
    private Runnable mUpdateMicStatusTimer = new Runnable() {
        @Override
        public void run() {
            updateMicStatus();
        }
    };

    /*
     * 更新麦克风状态
     * */
    private void updateMicStatus () {

        if (mMediaRecorder != null) {
            //振幅
            double ratio = (double)mMediaRecorder.getMaxAmplitude()/BASE;
            double db = 0;
            if (ratio > 1) {
                db = 20 * Math.log10(ratio);

                KLog.i("db======" + db);
                if (null != audioStatusUpdateListener) {
                    audioStatusUpdateListener.onUpdate(db, (System.currentTimeMillis() - startTime)/1000);
                }
            }
            mHandler.postDelayed(mUpdateMicStatusTimer, SPACE);
        }
    }


    public void setOnAudioStatusUpdateListener(OnAudioStatusUpdateListener audioStatusUpdateListener) {
        this.audioStatusUpdateListener = audioStatusUpdateListener;
    }

    public interface OnAudioStatusUpdateListener {

        void onUpdate(double db, long time);

        void onStop(long time, String filePath, String audioName);

        void onError();
    }
}
