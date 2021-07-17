package io.github.ornelasf1.mediaqueue;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import quickscroll.Scrollable;

/**
 * Created by Defsin on 5/23/2017.
 */

public class SongAdapter extends BaseAdapter implements Scrollable{
    private ArrayList<Song> songs;
    private LayoutInflater songInfo;
    private Context mContext;
    public static ArrayList<SongCheckable> songsCheck;
    private Typeface songFont, artistFont;
    ImageView testImg;

    public SongAdapter(Context c, ArrayList<Song> theSongs){
        songs = theSongs;
        mContext = c;
        songInfo = LayoutInflater.from(c);
        songFont = Typeface.createFromAsset(mContext.getResources().getAssets(), "Oswald-Regular.ttf");
        artistFont = Typeface.createFromAsset(mContext.getResources().getAssets(), "Oswald-Light.ttf");


        songsCheck = new ArrayList<SongCheckable>(songs.size());
        for(int i = 0; i < songs.size(); i++){
            songsCheck.add(new SongCheckable());
            songsCheck.get(i).setPosition(i);
            songsCheck.get(i).setChecked(false);
        }
    }

    public static boolean isHighlighted(){
        return false;
    }


    @Override
    public int getCount(){
        return songs.size();
    }

    @Override
    public Object getItem(int arg0){
        return null;
    }

    @Override
    public long getItemId(int arg0){
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        LinearLayout songLay = (LinearLayout)songInfo.inflate(R.layout.song, parent, false);

        TextView songView = (TextView)songLay.findViewById(R.id.song_title);
        TextView artistView = (TextView)songLay.findViewById(R.id.song_artist);

        final Song currSong = songs.get(position);

        songView.setText(currSong.getTitle());
        artistView.setText(currSong.getArtist());
        songView.setTypeface(songFont);
        artistView.setTypeface(artistFont);

        songLay.setTag(position);
        songLay.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.d("LOG", currSong.getTitle()+ " by " +currSong.getArtist());
                Log.d("LOG", "Here's long click");
                return false;
            }
        });

        if(songsCheck.get(position).isChecked()){
            songLay.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.selected_song_highlight_solid));
        }

        return songLay;
    }

    @Override
    public String getIndicatorForPosition(int childposition, int groupposition) {
        Log.d("LOG", "SongAdapter: String: " + songs.get(childposition).getTitle());
        if(MainActivity.sortedByTitle)return String.valueOf(songs.get(childposition).getTitle().charAt(0)).toUpperCase();
        else return String.valueOf(songs.get(childposition).getArtist().charAt(0)).toUpperCase();
    }

    @Override
    public int getScrollPosition(int childposition, int groupposition) {
        Log.d("LOG", "SongAdapter: childPos: " + childposition + "groupPos: " + groupposition);
        return childposition;
    }
}
