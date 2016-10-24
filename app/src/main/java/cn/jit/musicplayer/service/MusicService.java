package cn.jit.musicplayer.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import cn.jit.musicplayer.global.Constants;
import cn.jit.musicplayer.utils.MusicUtils;

/**
 * Created by QZ on 2016/10/22.
 * 音乐播放服务
 */

public class MusicService extends Service implements MediaPlayer.OnErrorListener, MediaPlayer
        .OnPreparedListener, MediaPlayer.OnCompletionListener, AudioManager
        .OnAudioFocusChangeListener {

    private MediaPlayer mPlayer;
    private Messenger mMessenger;
    private Timer mTimer;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        mPlayer = new MediaPlayer();
        mPlayer.setOnErrorListener(this);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnCompletionListener(this);
        //创建audioManger 处理音频焦点问题 this表示实现 OnAudioFocusChangeListener监听
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager
                .AUDIOFOCUS_GAIN);
        super.onCreate();
    }

    //此处通过onStartCommand获取intent中传递值的方式调用服务中的方法
    // 不通过绑定服务的方式调用服务中的方法
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //获取activity传递过来的messager，用于向activity发送消息
        if (mMessenger == null) {
            mMessenger = (Messenger) intent.getExtras().get("messenger");
        }
        //获取activity传递过来的操作
        String option = intent.getStringExtra("option");
        if (option.equals("play")) {
            String path = intent.getStringExtra("path");
            System.out.println("path" + path);
            play(path);
        } else if (option.equals("pause")) {
            pause();
        } else if (option.equals("continue")) {
            continuePlay();
        } else if (option.equals("stop")) {
            stop();
        } else if (option.equals("seek")) {
            int progress = intent.getIntExtra("progress", -1);
            seekPlay(progress);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mPlayer.release();
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        super.onDestroy();
    }

    //播放音乐
    public void play(String path) {
        try {
            mPlayer.reset();
            mPlayer.setDataSource(path);
            mPlayer.prepare();
            mPlayer.start();
            MusicUtils.CURSTATE = Constants.STATE_PLAY;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //暂停
    public void pause() {
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.pause();
            MusicUtils.CURSTATE = Constants.STATE_PAUSE;
        }
    }

    //继续播放
    public void continuePlay() {
        if (mPlayer != null && !mPlayer.isPlaying()) {
            mPlayer.start();
            MusicUtils.CURSTATE = Constants.STATE_PLAY;
        }
    }

    //停止播放
    public void stop() {
        if (mPlayer != null) {
            mPlayer.stop();
            MusicUtils.CURSTATE = Constants.STATE_STOP;
            if (mTimer != null) {
                mTimer.cancel();
                mTimer = null;
            }
        }
    }

    //拖拽进度播放
    public void seekPlay(int progress) {
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.seekTo(progress);
        }
    }

    //音乐准备完成 每隔一秒向activity发送当前播放进度的消息
    @Override
    public void onPrepared(MediaPlayer mp) {
        if (mTimer == null) {
            mTimer = new Timer();
        }
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    int currentDuration = mPlayer.getCurrentPosition();
                    int totalDuration = mPlayer.getDuration();
                    Message msg = Message.obtain();
                    msg.what = Constants.MSG_ONPREPARED;
                    msg.arg1 = currentDuration;
                    msg.arg2 = totalDuration;

                    mMessenger.send(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1000);
    }


    //资源加载出错
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        //Toast.makeText(this,"资源有问题",Toast.LENGTH_SHORT).show();
        return true;
    }

    //音乐播放完
    @Override
    public void onCompletion(MediaPlayer mp) {
        System.out.println("播放完成喽！！！");
        try {
            Message msg = Message.obtain();
            msg.what = Constants.MSG_ONCOMPLETE;
            mMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    //处理音频焦点问题,比如正在播放音乐,有电话打进来,或者点开其他视频,音频等，
    //音乐音频失去焦点,就应该停止播放
    //挂断电话,关闭视频,重新获得焦点,继续播放
    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN://获得音频焦点,继续播放音乐
                mPlayer.start();
                mPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS://失去了音频焦点很长时间了,停止所有的音频播放
                if (mPlayer.isPlaying())
                    mPlayer.stop();
                mPlayer.release();
                mPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT://暂时失去了音频焦点,暂停播放
                if (mPlayer.isPlaying())
                    mPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK://暂时失去了音频焦点，但可以小声地继续播放音频（低音量）而不是完全扼杀音频。
                if (mPlayer.isPlaying())
                    mPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }
}
