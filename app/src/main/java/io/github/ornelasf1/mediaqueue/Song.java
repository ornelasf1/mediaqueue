package io.github.ornelasf1.mediaqueue;

/**
 * Created by Defsin on 5/23/2017.
 */

public class Song {
    private long id;
    private String title;
    private String artist;
    private int songPosition;

    public Song(long songId, String songTitle, String songArtist){
        id = songId;
        title = songTitle;
        artist = songArtist;
    }

    public long getID(){return id;}
    public String getTitle(){return title;}
    public String getArtist(){return artist;}

    public void setPos(int pos){
        songPosition = pos;
    }

    public int getPos(){
        return songPosition;
    }

}
