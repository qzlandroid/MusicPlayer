package cn.jit.musicplayer;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import butterknife.ButterKnife;
import butterknife.InjectView;
import cn.jit.musicplayer.adapter.MyAdapter;
import cn.jit.musicplayer.beans.Music;
import cn.jit.musicplayer.global.Constants;
import cn.jit.musicplayer.service.MusicService;
import cn.jit.musicplayer.utils.LrcUtil;
import cn.jit.musicplayer.utils.MusicUtils;
import cn.jit.musicplayer.utils.ToastUtils;
import cn.jit.musicplayer.views.LyricShow;
import cn.jit.musicplayer.views.ScrollableViewGroup;

import static android.R.string.no;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    @InjectView(R.id.ib_top_play)
    ImageButton ibTopPlay;
    @InjectView(R.id.ib_top_list)
    ImageButton ibTopList;
    @InjectView(R.id.ib_top_lrc)
    ImageButton ibTopLrc;
    @InjectView(R.id.ib_top_volumn)
    ImageButton ibTopVolumn;
    @InjectView(R.id.ib_bottom_model)
    ImageButton ibBottomModel;
    @InjectView(R.id.ib_bottom_last)
    ImageButton ibBottomLast;
    @InjectView(R.id.ib_bottom_play)
    ImageButton ibBottomPlay;
    @InjectView(R.id.ib_bottom_next)
    ImageButton ibBottomNext;
    @InjectView(R.id.ib_bottom_update)
    ImageButton ibBottomUpdate;
    @InjectView(R.id.tv_minilrc)
    TextView tvMinilrc;
    @InjectView(R.id.tv_curduration)
    TextView tvCurduration;
    @InjectView(R.id.tv_totalduration)
    TextView tvTotalduration;
    @InjectView(R.id.sk_duration)
    SeekBar skDuration;
    @InjectView(R.id.lv_list)
    ListView lvList;
    @InjectView(R.id.tv_lrc)
    LyricShow tvLrc;
    @InjectView(R.id.svg_main)
    ScrollableViewGroup svgMain;
    @InjectView(R.id.iv_bottom_play)
    ImageView mIvPlay;
    @InjectView(R.id.iv_bottom_model)
    ImageView mIvModel;

    private ArrayList<ImageButton> mTopIbs = new ArrayList<>();
    private ArrayList<Music> musics;
    private LrcUtil mLrcUtil;
    private MainActivity mInstance;
    private String musicName;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case Constants.MSG_ONPREPARED:
                    int currentDuration = msg.arg1;
                    int totalDuration = msg.arg2;
                    tvCurduration.setText(MusicUtils.duration2Str(currentDuration));
                    tvTotalduration.setText(MusicUtils.duration2Str(totalDuration));
                    skDuration.setMax(totalDuration);
                    skDuration.setProgress(currentDuration);

                    if (mLrcUtil == null){
                        mLrcUtil = new LrcUtil(mInstance);
                    }
                    File file = MusicUtils.getLrcFile(musics.get(MusicUtils.CURPOSITION).path);
                    mLrcUtil.ReadLRC(file);
                    mLrcUtil.RefreshLRC(currentDuration);
                    tvLrc.SetTimeLrc(LrcUtil.lrclist);
                    tvLrc.SetNowPlayIndex(currentDuration);
                    break;
                case Constants.MSG_ONCOMPLETE:

                    //根据当前播放模式做处理
                    if (MusicUtils.CURMODEL == Constants.MODEL_NORMAL){ //当前是顺序播放
                        if (MusicUtils.CURPOSITION < musics.size() - 1) {
                            changeListColor(Color.WHITE);
                            MusicUtils.CURPOSITION++;
                            changeListColor(Color.GREEN);
                            startMusicService("play", musics.get(MusicUtils.CURPOSITION).path, -1);
                        }else {
                            startMusicService("stop","",-1);
                        }
                    }else if (MusicUtils.CURMODEL == Constants.MODEL_RANDOM){ //当前是随机播放
                        Random random = new Random();
                        int position = random.nextInt(musics.size());
                        changeListColor(Color.WHITE);
                        MusicUtils.CURPOSITION = position;
                        changeListColor(Color.GREEN);
                        startMusicService("play",musics.get(MusicUtils.CURPOSITION).path,-1);

                    }else if (MusicUtils.CURMODEL == Constants.MODEL_REPEAT){   //当前是重复播放
                        changeListColor(Color.WHITE);
                        if (MusicUtils.CURPOSITION < musics.size() - 1) {
                            MusicUtils.CURPOSITION++;
                        }else {
                            MusicUtils.CURPOSITION = 0;
                        }
                        changeListColor(Color.GREEN);
                        startMusicService("play", musics.get(MusicUtils.CURPOSITION).path, -1);
                    }else if (MusicUtils.CURMODEL == Constants.MODEL_SINGLE){   //当前是单曲循环
                        startMusicService("play",musics.get(MusicUtils.CURPOSITION).path,-1);
                    }
                    break;
                default:
                    break;
            }
        }
    };
    private ProgressDialog dialog;
    private MyAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        initView();
        initData();
        initListener();
    }


    private void initView() {
        tvLrc.setLrcTextSize(40);
        tvLrc.setCurrentTextSize(60);
        tvLrc.setTextHeight(80);
    }

    private void initData() {
        mInstance = this;
        mTopIbs.add(ibTopPlay);
        mTopIbs.add(ibTopLrc);
        mTopIbs.add(ibTopList);
        musics = MusicUtils.initMusicData(this);
        mAdapter = new MyAdapter(this, musics);
        if (musics != null) {
            lvList.setAdapter(mAdapter);
        }
        musicName = musics.get(MusicUtils.CURPOSITION).title;
    }

    private void initListener() {
        ibTopPlay.setOnClickListener(this);
        ibTopList.setOnClickListener(this);
        ibTopLrc.setOnClickListener(this);
        ibBottomPlay.setOnClickListener(this);
        ibBottomNext.setOnClickListener(this);
        ibBottomLast.setOnClickListener(this);
        ibBottomModel.setOnClickListener(this);
        ibBottomUpdate.setOnClickListener(this);
        ibTopVolumn.setOnClickListener(this);
        svgMain.setOnCurrentViewChangedListener(new ScrollableViewGroup.OnCurrentViewChangedListener() {

            @Override
            public void onCurrentViewChanged(View view, int currentview) {
                changeTopSelected(currentview);
            }
        });
        lvList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                changeListColor(Color.WHITE);
                MusicUtils.CURPOSITION = position;
                changeListColor(Color.GREEN);
                startMusicService("play",musics.get(position).path,-1);
                mIvPlay.setImageResource(R.mipmap.appwidget_pause);
            }
        });
        skDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                skDuration.setProgress(seekBar.getProgress());
                startMusicService("seek","",seekBar.getProgress());
            }
        });
    }



    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.ib_top_play:
                svgMain.setCurrentView(0);
                changeTopSelected(0);
                break;
            case R.id.ib_top_lrc:
                svgMain.setCurrentView(1);
                changeTopSelected(1);
                break;
            case R.id.ib_top_list:
                svgMain.setCurrentView(2);
                changeTopSelected(2);
                break;
            case R.id.ib_top_volumn:
                setVolume();
                break;
            case R.id.ib_bottom_play:
                if (MusicUtils.CURSTATE == Constants.STATE_STOP){
                    startMusicService("play",musics.get(MusicUtils.CURPOSITION).path,-1);
                    mIvPlay.setImageResource(R.mipmap.appwidget_pause);
                }else if (MusicUtils.CURSTATE == Constants.STATE_PLAY){
                    startMusicService("pause","",-1);
                    mIvPlay.setImageResource(R.mipmap.img_playback_bt_play);
                }else if (MusicUtils.CURSTATE == Constants.STATE_PAUSE){
                    startMusicService("continue","",-1);
                    mIvPlay.setImageResource(R.mipmap.appwidget_pause);
                }
                break;
            case R.id.ib_bottom_last:
                if (MusicUtils.CURPOSITION > 0){
                    changeListColor(Color.WHITE);
                    MusicUtils.CURPOSITION--;
                    changeListColor(Color.GREEN);
                    startMusicService("play",musics.get(MusicUtils.CURPOSITION).path,-1);
                    mIvPlay.setImageResource(R.mipmap.appwidget_pause);
                }
                break;
            case R.id.ib_bottom_next:
                if (MusicUtils.CURPOSITION < musics.size() - 1){
                    changeListColor(Color.WHITE);
                    MusicUtils.CURPOSITION++;
                    changeListColor(Color.GREEN);
                    startMusicService("play",musics.get(MusicUtils.CURPOSITION).path,-1);
                    mIvPlay.setImageResource(R.mipmap.appwidget_pause);
                }
                break;
            case R.id.ib_bottom_model:
                if (MusicUtils.CURMODEL == Constants.MODEL_NORMAL){ //当前是顺序播放
                    MusicUtils.CURMODEL = Constants.MODEL_RANDOM;   //改为随机播放
                    mIvModel.setImageResource(R.mipmap.icon_playmode_shuffle);
                    ToastUtils.showToast(MainActivity.this,"随机播放",500);
                }else if (MusicUtils.CURMODEL == Constants.MODEL_RANDOM){ //当前是随机播放
                    MusicUtils.CURMODEL = Constants.MODEL_REPEAT;   //改为重复播放
                    mIvModel.setImageResource(R.mipmap.icon_playmode_repeat);                    ToastUtils.showToast(MainActivity.this,"随机播放",500);
                    ToastUtils.showToast(MainActivity.this,"重复播放",500);
                }else if (MusicUtils.CURMODEL == Constants.MODEL_REPEAT){   //当前是重复播放
                    MusicUtils.CURMODEL = Constants.MODEL_SINGLE;   //改为单曲循环
                    mIvModel.setImageResource(R.mipmap.icon_playmode_single);
                    ToastUtils.showToast(MainActivity.this,"单曲循环",500);
                }else if (MusicUtils.CURMODEL == Constants.MODEL_SINGLE){   //当前是单曲循环
                    MusicUtils.CURMODEL = Constants.MODEL_NORMAL;   //改为顺序播放
                    mIvModel.setImageResource(R.mipmap.icon_playmode_normal);
                    ToastUtils.showToast(MainActivity.this,"顺序播放",500);
                }
                break;
            case R.id.ib_bottom_update:
                reflash();
                break;
            default:
                break;
        }
    }
    //修改顶部按钮选中状态
    private void changeTopSelected(int position){
        for(ImageButton ib : mTopIbs){
            ib.setSelected(false);
        }
        mTopIbs.get(position).setSelected(true);
    }

    public void startMusicService(String option,String path,int progress){
        Intent service = new Intent(MainActivity.this,MusicService.class);
        service.putExtra("messenger",new Messenger(mHandler));
        service.putExtra("option",option);
        service.putExtra("path",path);
        service.putExtra("progress",progress);
        startService(service);
        musicName = musics.get(MusicUtils.CURPOSITION).title;
    }

    //修改音乐列表的颜色
    public void changeListColor(int color){
        TextView tv = (TextView) lvList.findViewWithTag(MusicUtils.CURPOSITION);
        if (tv != null){
            tv.setTextColor(color);
        }
    }

    public void setMiniLrc(String string){
        tvMinilrc.setText(string);
    }
    /**
     * 1.发送特定的广播,让操作系统更新多媒体数据
     * 2.系统扫描完成,会发出一个特定的的广播.我们只需要去监听特定的广播
     */
    public void reflash() {
        /**---------------接收系统扫描完成的广播---------------**/
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        filter.addDataScheme("file");
        //注册广播
        registerReceiver(receiver, filter);

        /**---------------发送广播,让系统更新媒体数据库---------------**/
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.parse("file://" + Environment.getExternalStorageDirectory()));
        sendBroadcast(intent);
    }

    MyBroadcastReceiver receiver = new MyBroadcastReceiver();

    class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {//onReceive这个方法里面不应该执行耗时的操作
            //反注册广播
            unregisterReceiver(receiver);
            //执行task
            new MyAsyncTask().execute();
        }
    }

    class MyAsyncTask extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... params) {
            MusicUtils.initMusicData(MainActivity.this);
            return null;
        }

        @Override
        protected void onPreExecute() {
            dialog = ProgressDialog.show(MainActivity.this,"提示","努力更新中");
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            dialog.dismiss();
            mAdapter.notifyDataSetChanged();
            super.onPostExecute(aVoid);
        }
    }

    //设置音量
    private void setVolume(){
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,maxVolume / 2,AudioManager.FLAG_PLAY_SOUND);
    }

    /**
     * 点击返回键,不销毁activity,而是发送一个跳转桌面的隐式意图
     * 注意：此时在重新打开应用会发现页面出现bug,因为activity重新执行了OnCreate方法
     * 所以可以把activity的设计模式改为singleTask
     */
    
    @Override
    public void onBackPressed() {
        //跳转到桌面
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(intent.CATEGORY_HOME);
        startActivity(intent);
        //显示notification
        showNotification();
    }

    private void showNotification() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder notification = new Notification.Builder(this);
        notification.setAutoCancel(true);
        notification.setContentTitle("MusicPlayer");
        notification.setContentText(musics.get(MusicUtils.CURPOSITION).title);
        Intent intent = new Intent(this,MainActivity.class);
        PendingIntent  contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setContentIntent(contentIntent);
        notification.setSmallIcon(R.mipmap.ic_launcher);
        notification.setWhen(System.currentTimeMillis());
        notification.build();

        Notification builder = notification.build();
        mNotificationManager.notify(2, builder);
    }
}
