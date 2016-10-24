package cn.jit.musicplayer.global;

/**
 * Created by QZ on 2016/10/22.
 */

public class Constants {
    public static final int STATE_STOP = 1; //停止状态
    public static final int STATE_PLAY = 2; //播放状态
    public static final int STATE_PAUSE = 3; //暂停状态

    public static final int MSG_ONPREPARED = 4; //播放就绪消息
    public static final int MSG_ONCOMPLETE = 5; //播放完成消息

    public static final int MODEL_NORMAL = 6; //顺序播放
    public static final int MODEL_REPEAT = 7; //重复播放
    public static final int MODEL_SINGLE = 8; //单曲循环
    public static final int MODEL_RANDOM = 9; //随机播放

}
