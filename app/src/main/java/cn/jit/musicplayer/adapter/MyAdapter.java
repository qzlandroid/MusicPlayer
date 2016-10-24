package cn.jit.musicplayer.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import cn.jit.musicplayer.R;
import cn.jit.musicplayer.beans.Music;
import cn.jit.musicplayer.utils.MusicUtils;

/**
 * Created by QZ on 2016/10/21.
 */

public class MyAdapter extends BaseAdapter {
    private ArrayList<Music> musics;
    private Context context;

    public MyAdapter(Context context,ArrayList<Music> musics){
        this.context = context;
        this.musics = musics;
    }
    @Override
    public int getCount() {
        return musics.size();
    }

    @Override
    public Music getItem(int position) {
        return musics.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null){
            holder = new ViewHolder();
            convertView = View.inflate(context, R.layout.item_music,null);
            holder.tv_title = (TextView) convertView.findViewById(R.id.tv_title);
            holder.tv_artist = (TextView) convertView.findViewById(R.id.tv_artist);
            convertView.setTag(holder);
        }else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.tv_title.setText(getItem(position).title);
        holder.tv_artist.setText(getItem(position).artist);

        if (MusicUtils.CURPOSITION == position){
            holder.tv_title.setTextColor(Color.GREEN);
        }else {
            holder.tv_title.setTextColor(Color.WHITE);
        }
        holder.tv_title.setTag(position);
        return convertView;
    }
    class ViewHolder{
        TextView tv_title;
        TextView tv_artist;
    }
}
