package io.github.ornelasf1.mediaqueue;

import android.app.Service;
import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Defsin on 5/28/2017.
 */

public class Queue extends BaseAdapter{
    private Song songEntry;
    static public boolean queueActive = false;
    static public ArrayList<Song> songQueue = new ArrayList<Song>();
    private MusicService queueMusic;
    private Context mContext;
    private LinearLayout songLay;

    private LayoutInflater songInfo;

    public Queue(){}

    public Queue(Context c, Song song){
        mContext = c;
        songEntry = song;
        if(songEntry != null && !songQueue.contains(songEntry)){
            songQueue.add(songEntry);
        }
        else{
            Log.d("LOG","songEntry is null or songEntry already inside");
        }
        songInfo = LayoutInflater.from(c);
    }

    public void nextSong(){
        songQueue.remove(0);

    }

    public void clearQueue(){
        songQueue.clear();


    }

    @Override
    public int getCount() {
        return songQueue.size();
    }

    @Override
    public Object getItem(int position) {
        return songQueue.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        songLay = (LinearLayout)songInfo.inflate(R.layout.nav_main_song, parent, false);

        TextView songView = (TextView)songLay.findViewById(R.id.queue_song_title);
        TextView artistView = (TextView)songLay.findViewById(R.id.queue_song_artist);

        final Song currSong = songQueue.get(position);
        final int songPos = songQueue.get(position).getPos();

        songView.setText(currSong.getTitle());
        artistView.setText(currSong.getArtist());
        songView.setTypeface(Typeface.createFromAsset(mContext.getAssets(), OswaldFont.LIGHT));
        artistView.setTypeface(Typeface.createFromAsset(mContext.getAssets(), OswaldFont.EXTRALIGHT));

        songLay.setTag(position);
        songLay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("LOG", "QUEUE: " + currSong.getTitle()+ " by " +currSong.getArtist());
                /*queueMusic.setSong(songPos); NOT ALLOWED TO PLAY SONG FROM QUEUE, I THINK
                queueMusic.playSong();*/
                Log.d("LOG", "Here's click FROM QUEUE: SongPos is "+ songQueue.get(position).getPos());
            }
        });

        songLay.setOnTouchListener(new SwipeDismissTouchListener(songLay, null, new SwipeDismissTouchListener.DismissCallbacks() {
            @Override
            public boolean canDismiss(Object token) {
                return true;
            }

            @Override
            public void onDismiss(View view, Object token) {
                Log.d("LOG", "QUEUE: onDismiss in QUEUE");
                songQueue.remove(position);
                if(songQueue.isEmpty()){
                    MainActivity.bChangLis.updateNavView();
                }
            }
        }));


        return songLay;
    }
}
