package cn.jit.musicplayer.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.jit.musicplayer.beans.Music;
import cn.jit.musicplayer.global.Constants;

/**
 * Created by QZ on 2016/10/21.
 * 获取本地音乐数据
 */

public class MusicUtils {

    public static ArrayList<Music> songLists = new ArrayList<>();
    public static int CURSTATE = Constants.STATE_STOP; //音乐当前状态
    public static int CURPOSITION = 0; //当前播放哪首音乐
    public static int CURMODEL = Constants.MODEL_NORMAL; //当前模式


    public static ArrayList<Music> initMusicData(Context context){

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.ARTIST,MediaStore.Audio.Media.DATA};
        Cursor c = context.getContentResolver().query(uri, projection, null, null, null);
        while (c.moveToNext()){
            String title = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE));
            String artist = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            String path = c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATA));
            Music music = new Music(title,artist,path);
            songLists.add(music);
        }
        return songLists;
    }

    //格式化歌曲时长
    public static String duration2Str(int duration) {
        String result;
        int i = duration / 1000;
        int min = i / 60;//1 2  3
        int sec = i % 60;// 0-59
        if (min > 9) {
            if (sec > 9) {
                result = min + ":" + sec;
            } else {
                result = min + ":0" + sec;
            }
        } else {
            if (sec > 9) {
                result = "0" + min + ":" + sec;
            } else {
                result = "0" + min + ":0" + sec;
            }
        }
        return result;
    }

    /**
     * 根据歌曲的路径,得到对应的lrc
     * @param path
     * @return
     */
    public static File getLrcFile(String path) {
        File file;
        String lrcName = path.replace(".mp3", ".lrc");//找歌曲名称相同的.lrc文件
        file = new File(lrcName);
        if (!file.exists()) {
            lrcName = path.replace(".mp3", ".txt");//歌词可能是.txt结尾
            file = new File(lrcName);
            if (!file.exists()) {
                return null;
            }
        }
        return file;

    }
}
