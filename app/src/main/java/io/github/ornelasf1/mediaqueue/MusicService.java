package io.github.ornelasf1.mediaqueue;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.transition.Visibility;
import android.support.v4.app.BundleCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextSwitcher;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Defsin on 5/23/2017.
 */

public class MusicService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener, HeadsetReceiver.HeadsetInterface{
    private MediaPlayer player;
    private ArrayList<Song> songs;
    private int songPos;
    private final IBinder musicBind = new MusicBinder();
    private Context musicSrvContext;
    private ListView songListView;
    NotificationCompat.Builder notificationBuilder;

    private String songTitle="";
    private String songArtist="";
    private static final int NOTIFY_ID=1;

    private boolean shuffle = true;
    private Random rand;
    static ArrayDeque<Integer> songPositionHistory;

    public boolean isSongCompleted;

    private TextSwitcher songSwitch;

    private AudioManager mAudioMan;
    int audResult;

    private HeadsetReceiver headsetReceiver;

    public void setTextSwitcher(TextSwitcher tS){
        songSwitch = tS;
    }



    @Override
    public IBinder onBind(Intent intent){
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        IntentFilter filterBT1 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        IntentFilter filterBT2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        registerReceiver(headsetReceiver, filter);
        registerReceiver(headsetReceiver, filterBT1);
        registerReceiver(headsetReceiver, filterBT2);
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent){
        Log.d("LOG", "MusicService: onUnBind");
        player.stop();
        player.release();
        unregisterReceiver(headsetReceiver);
        return super.onUnbind(intent);
    }

    public void onCreate(){
        super.onCreate();
        Log.d("LOG", "MusicService: onCreate()");
        songPos = 0;
        /*player = new MediaPlayer();*/

        notificationBuilder = null;
        initMusicPlayer();
        rand = new Random();
        //songSwitch = MainActivity.songSwitch;

        mAudioMan = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        audResult = mAudioMan.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        songPositionHistory = new ArrayDeque<Integer>();

        headsetReceiver = new HeadsetReceiver();
        headsetReceiver.registerListener(this);
    }

    @Override
    public void onDestroy(){
        Log.d("LOG", "MusicService: onDestroy");
        stopForeground(true);
        removeNotification();
    }

    public boolean ifRetrievedAudioFocus(){
        mAudioMan = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        audResult = mAudioMan.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        return audResult == AudioManager.AUDIOFOCUS_GAIN;
    }

    public void initMusicPlayer(){
        Log.d("LOG", "MusicService: initMusicPlayer()");
        player = new MediaPlayer();
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);

        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    public void setContext(Context c){
        musicSrvContext = c;
    }

    public void setSongListView(ListView slv){songListView = slv;}

    public void setList(ArrayList<Song> theSongs){
        songs = theSongs;
    }

    public void playSong(){
        Log.d("LOG", "MusicService: playSong()");
        player.reset();
        Song playSong = songs.get(songPos);
        songTitle = playSong.getTitle();
        songArtist = playSong.getArtist();
        long currSong = playSong.getID();
        Uri trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currSong);
        try{
            player.setDataSource(getApplicationContext(), trackUri);
        }catch(Exception e){
            Log.e("Media Queue","Error setting data source", e);
        }
        player.prepareAsync();
        MainActivity.bChangLis.notifyIfSongCompleted(false);
        displaySongOnToolBar(songTitle, songArtist);

        clearChecksAndHighlight(songPos);
        MainActivity.onStartBool = true;
        /*songs.get(songPos).getSongView().setBackgroundColor(getBaseContext().getResources().getColor(R.color.colorPrimaryDark));*/


    }

    public void clearChecksAndHighlight(int pos){
        int size = SongAdapter.songsCheck.size();
        for(int i = 0; i < size; i++){
            if(SongAdapter.songsCheck.get(i).isChecked()){
                SongAdapter.songsCheck.get(i).setChecked(false);
            }
        }
        SongAdapter.songsCheck.get(pos).setChecked(true);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d("LOG", "MusicService: song onCompletion()");
        if(player.getCurrentPosition() > 0){
            mp.reset();
            if(!Queue.queueActive){
                Log.d("LOG", "Queue is NOT ACTIVE");
                playNext();
            }else{
                Log.d("LOG", "Queue is ACTIVE");
                if(!Queue.songQueue.isEmpty()) {
                    Log.d("LOG", "Queue is NOT EMPTY");
                    songPos = Queue.songQueue.get(0).getPos();
                    Queue.songQueue.remove(0);
                    playSong();
                }else{
                    Log.d("LOG", "Queue is EMPTY, TURNING OFF ACTIVE");
                    Queue.queueActive = false;
                    stopOnEmptyQueue();
                    playNext();

                }
            }
        }
        MainActivity.bChangLis.notifyIfSongCompleted(true);
    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d("LOG", "MusicService: onPrepared() MediaPlayer");
        mp.start();

        /*Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.mipmap.mediaqueue_logo_green)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Up in this bitch, we got")
                .setContentText(songTitle + " by " + songArtist);
        Notification not = builder.build();

        startForeground(NOTIFY_ID, not);*/



        try {
            Log.d("LOG", "MusicService: onPrepare(): initMediaSession");
            initMediaSession();
        } catch (RemoteException e) {
            e.printStackTrace();
            stopSelf();
        }
        buildNotification(PlaybackStatus.PLAYING);
        //lockScreenNotification();

        //Handle Intent action from MediaSession.TransportControls
        /*handleIncomingActions(new Intent(this, MusicService.class));*/

        MainActivity.showController();
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch(focusChange){
            case AudioManager.AUDIOFOCUS_LOSS:
                Log.d("LOG", "MusicService: Audio was lost");
                pausePlayer();
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                Log.d("LOG", "MusicService: Audio was gained");
                if(!MainActivity.getPlaybackStatus()) go();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                pausePlayer();
                Log.d("LOG", "MusicService: Audio was lost transient");
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                Log.d("LOG", "MusicService: Audio was lost CAN DUCK");
                pausePlayer();
                break;
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                if(!MainActivity.getPlaybackStatus()) go();
                Log.d("LOG", "MusicService: Audio was gained transient");
                break;
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                if(!MainActivity.getPlaybackStatus()) go();
                Log.d("LOG", "MusicService: Audio was gained transient MAYD DUCK");
                break;
            default:
                break;
        }
    }

    @Override
    public void checkHeadsetPluggedIn(boolean state) {
        if(state){
            Log.d("LOG", "MusicService: Headset plugged in");
        }else{
            Log.d("LOG", "MusicService: Headset NOT plugged in");
            if(player != null && isPng()) pausePlayer();
        }
    }

    public class MusicBinder extends Binder {
        MusicService getService(){
            Log.d("LOG", "MusicService(MusicBinder.class): getService()");
            try{
                initMediaSession();
                MainActivity.showController();
            }catch(RemoteException e){
                e.printStackTrace();
            }
            return MusicService.this;
        }
    }

    public void setSong(int songIndex){
        songPos = songIndex;
    }

    public Song getSong(int songIndex){ return songs.get(songIndex); }

    public int getSongPos(){return songPos;}

    public int getNextSongPos(){
        int lastSongPlayed;
        if(!songPositionHistory.isEmpty()) {
            lastSongPlayed = songPositionHistory.pop();
            for(Integer in:songPositionHistory){
                Log.d("LOG","MusicServie: List: " + in);
            }
        }else{
            lastSongPlayed = rand.nextInt(songs.size());
        }
        return lastSongPlayed;
    }

    public static void setNextSongPos(Integer i){
        songPositionHistory.push(i);
        for(Integer in:songPositionHistory){
            Log.d("LOG","MusicService: List: " + in);
        }
    }

    public void playPrev(){
        if(!Queue.queueActive) {
            if(!shuffle) {
                songPos--;
                if (songPos < 0) {
                    songPos = songs.size() - 1;
                }
                playSong();
            }else{
                songPos = getNextSongPos();
                playSong();
            }
        }else{
            //TODO What do when queue is active
            /*songPos = getNextSongPos();
            playSong();*/
        }
    }

    public void playNext(){
        Log.d("LOG", "MusicService: playNext()");
        if(!Queue.queueActive) {
            Log.d("LOG", "MusicService: playNext() - Queue is not active");
            if (shuffle) {
                Log.d("LOG", "MusicService: playNext() - Shuffle is ON");
                int newSong = songPos;
                newSong = rand.nextInt(songs.size());
                Log.d("LOG", "MusicService: While in playNext()");
                setNextSongPos(songPos);
                songPos = newSong;
            } else {
                Log.d("LOG", "MusicService: playNext() - Shuffle is OFF");
                songPos++;
                if (songPos >= songs.size()) {
                    songPos = 0;
                }
                Log.d("LOG", "MusicService: - songPos: " + songPos + " Size of songs: " + songs.size());
            }
        }else{
            Log.d("LOG", "MusicService: playNext() - Queue is active");
            if(!Queue.songQueue.isEmpty()) {
                //setNextSongPos(songPos);
                Log.d("LOG", "MusicService: playNext() - set songPos to next song in Queue");
                songPos = Queue.songQueue.get(0).getPos();
                Queue.songQueue.remove(0);
            }else{
                Log.d("LOG", "MusicService: playNext() - disable queueActive and invoke playNext()");
                Queue.queueActive = false;
                stopOnEmptyQueue();
                playNext();
            }
        }
        playSong();
    }

    public int getPosn(){
        return player.getCurrentPosition();
    }

    public int getDur(){
        return player.getDuration();
    }

    public boolean isPng(){
        return player.isPlaying();
    }

    public void pausePlayer(){
        player.pause();
        MainActivity.showController();
        buildNotification(PlaybackStatus.PAUSED);
    }

    public void seek(int posn){
        player.seekTo(posn);
    }

    public void go(){
        player.start();
        MainActivity.showController();
        MainActivity.onStartBool = true;
        buildNotification(PlaybackStatus.PLAYING);
    }

    public void setShuffle(){
        if(shuffle){
            Toast.makeText(this, "Shuffle OFF", Toast.LENGTH_SHORT).show();
            shuffle = false;
        }else{
            Toast.makeText(this, "Shuffle ON", Toast.LENGTH_SHORT).show();
            shuffle = true;
        }
    }

    public boolean getShuffle(){
        return shuffle;
    }

    public void stopOnEmptyQueue(){
        if(MainActivity.stopOnEndCheck){
            removeNotification();
            stopSelf();
            MainActivity.bChangLis.closeAppOnNotification();
        }
    }

    public void displaySongOnToolBar(String song, String artist){
        String outputSong = song;
        Animation in = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
        Animation out = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);

        songSwitch.setInAnimation(in);
        songSwitch.setOutAnimation(out);

        if(song.length() > 24) outputSong = song.substring(0, 23);
        if(artist.length() > 24) outputSong = artist.substring(0, 23);

        songSwitch.setText(outputSong + "\n" + artist);
    }

    private NotificationManager notificationManager;
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;

    private void initMediaSession() throws RemoteException{
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        /*if(mediaSessionManager != null) return; //mediaSessionManager exists*/

        //mediaSessionManager = (MediaSessionManager)getSystemService(Context.MEDIA_SESSION_SERVICE);

        mediaSession = new MediaSessionCompat(getApplicationContext(), "MediaQueue");
        transportControls = mediaSession.getController().getTransportControls();

        updateMetaData();

        mediaSession.setActive(true);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);


        Log.d("LOG", "MusicService: initMediaSession(): Implemeneted .setCallBack");
        mediaSession.setCallback(new MediaSessionCompat.Callback(){

            @Override
            public void onPlay() {
                super.onPlay();
                Log.d("LOG", "Pressed play on Notification");
                if(!ifRetrievedAudioFocus()){
                    Log.d("LOG", "Pressed play on Notification : ACCESS DENIED");
                    return;
                }
                MainActivity.setPlaybackStatus(false);
                go();
            }

            @Override
            public void onPause() {
                super.onPause();
                Log.d("LOG", "Pressed pause on Notification");
                if(!ifRetrievedAudioFocus()){
                    Log.d("LOG", "Pressed pause on Notification : ACCESS DENIED");
                    return;
                }
                MainActivity.setPlaybackStatus(true);
                pausePlayer();
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                if(!ifRetrievedAudioFocus()){
                    return;
                }
                playNext();
                updateMetaData();
                MainActivity.showController();
                MainActivity.bChangLis.notifyIfSongCompleted(true);
                buildNotification(PlaybackStatus.PLAYING);
                MainActivity.setPlaybackStatus(false);
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                if(!ifRetrievedAudioFocus()){
                    return;
                }
                playPrev();
                updateMetaData();
                MainActivity.showController();
                buildNotification(PlaybackStatus.PLAYING);
                MainActivity.setPlaybackStatus(false);
            }

            @Override
            public void onStop() {
                super.onStop();
                Log.d("LOG", "onSTOP is being called");
                removeNotification();
                //Stop the service
                stopSelf();
                MainActivity.bChangLis.closeAppOnNotification();
            }

            @Override
            public void onSeekTo(long pos) {
                super.onSeekTo(pos);
            }
        });
    }

    private void updateMetaData() {
        Bitmap albumArt = BitmapFactory.decodeResource(getResources(),
                R.mipmap.mediaqueue_logo_green);
        // Update the current metadata
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, songArtist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "No Album")
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, songTitle)
                .build());
    }


    public void buildNotification(PlaybackStatus playbackStatus){
        Log.d("LOG", "MusicService: buildNotification(): called with ");
        int notificationAction = android.R.drawable.ic_media_pause;
        PendingIntent play_pauseAction = null;

        if(playbackStatus == PlaybackStatus.PLAYING){
            Log.d("LOG", "PLAYING - Show Pause Icon");
            notificationAction = android.R.drawable.ic_media_pause;
            //create the pause action
            play_pauseAction = playbackAction(1);
        }else if(playbackStatus == PlaybackStatus.PAUSED){
            Log.d("LOG", "PAUSED - Show Play Icon");
            notificationAction = android.R.drawable.ic_media_play;
            //create the play action
            play_pauseAction = playbackAction(0);
        }

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent toMain = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.mediaqueue_logo_offic_two);

        NotificationCompat.Action actionButt = new NotificationCompat.Action(notificationAction, "pause", play_pauseAction);

        //Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.HONEYCOMB

        if (notificationBuilder == null) {
            Log.d("LOG", "MusicService: Creating notification!");
            notificationBuilder = new NotificationCompat.Builder(this);

            notificationBuilder
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setShowWhen(false)
                    // Set the Notification style
                    .setContentIntent(toMain)
                    .setStyle(new NotificationCompat.MediaStyle()
                            // Attach our MediaSession token
                            .setMediaSession(mediaSession.getSessionToken())
                            // Show our playback controls in the compact notification view.
                            .setShowActionsInCompactView(0, 1, 2)
                            .setShowCancelButton(true)
                            .setCancelButtonIntent(playbackAction(4)))
                    // Set the Notification color
                    .setColor(getResources().getColor(R.color.transparent))
                    // Set the large and small icons
                    .setLargeIcon(largeIcon)
                    .setSmallIcon(R.drawable.ic_mq_vector_icon)
                    // Set Notification content information
                    .setContentText(songArtist)
                    .setContentTitle(songTitle)
                    // Add playback actions
                    .addAction(android.R.drawable.ic_media_previous, "previous", playbackAction(3))
                    .addAction(actionButt)
                    .addAction(android.R.drawable.ic_media_next, "next", playbackAction(2))
                    .setOngoing(true);

        } else {
            Log.d("LOG", "MusicService: Updating notification!");
            notificationBuilder.mActions.set(1, actionButt);
            notificationBuilder
                    .setContentText(songArtist)
                    .setContentTitle(songTitle)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }
        notificationManager.notify(NotificationConstants.NOTIFICATION_ID.FOREGROUND_SERVICE, notificationBuilder.build());
    }

    public void lockScreenNotification(){
        NotificationCompat.Builder lockNotification = new NotificationCompat.Builder(this);

        lockNotification
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_menu_gallery)
                .addAction(R.drawable.apollo_holo_dark_next, "next", playbackAction(2))
                .setStyle(new NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0)
                    .setMediaSession(mediaSession.getSessionToken()))
                .setContentTitle("Wonderful music")
                .setContentText("My awrome band")
                .setLargeIcon(NotificationConstants.getDefaultAlbumArt(this));

        notificationManager.notify(NotificationConstants.NOTIFICATION_ID.FOREGROUND_SERVICE, lockNotification.build());
        Log.d("LOG", "MusicService: NEW NOTIFICATION BUILT");
    }

    private void removeNotification(){
        if(notificationManager != null) notificationManager.cancel(NotificationConstants.NOTIFICATION_ID.FOREGROUND_SERVICE);
    }

    private PendingIntent playbackAction(int actionNumber) {
        Log.d("LOG", "MusicService: playbackAction: called");
        Intent playbackAction = new Intent(this, MusicService.class);
        switch (actionNumber) {
            case 0:
                // Play
                playbackAction.setAction(NotificationConstants.ACTION.ACTION_PLAY);
                Log.d("LOG", "playbackAction: setAction ACTION_PLAY");
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                // Pause
                playbackAction.setAction(NotificationConstants.ACTION.ACTION_PAUSE);
                Log.d("LOG", "playbackAction: setAction ACTION_PAUSE");
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                // Next track
                playbackAction.setAction(NotificationConstants.ACTION.ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                // Previous track
                playbackAction.setAction(NotificationConstants.ACTION.ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 4:
                playbackAction.setAction(NotificationConstants.ACTION.ACTION_STOP);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 5:
                playbackAction.setAction(NotificationConstants.ACTION.ACTION_ENTER);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    private void handleIncomingActions(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        Log.d("LOG", "handleIncomingActions: Intent's getAction: " + actionString);
        if (actionString.equalsIgnoreCase(NotificationConstants.ACTION.ACTION_PLAY)) {
            Log.d("LOG", "actionString equals.ACTION_PLAY");
            transportControls.play();
        } else if (actionString.equalsIgnoreCase(NotificationConstants.ACTION.ACTION_PAUSE)) {
            transportControls.pause();
            Log.d("LOG", "actionString equals.ACTION_PAUSE");
        } else if (actionString.equalsIgnoreCase(NotificationConstants.ACTION.ACTION_NEXT)) {
            transportControls.skipToNext();
            Log.d("LOG", "actionString equals.ACTION_NEXT");
        } else if (actionString.equalsIgnoreCase(NotificationConstants.ACTION.ACTION_PREVIOUS)) {
            transportControls.skipToPrevious();
            Log.d("LOG", "actionString equals.ACTION_PREVIOUS");
        } else if (actionString.equalsIgnoreCase(NotificationConstants.ACTION.ACTION_STOP)) {
            Log.d("LOG", "actionString equals.ACTION_STOP");
            transportControls.stop();
        } else if(actionString.equalsIgnoreCase(NotificationConstants.ACTION.ACTION_ENTER)){
            Log.d("LOG", "actionString equals.ACTION_ENTER");
        }
    }

    @RequiresApi(20)
    private Notification.Action generateAction(int icon, String title, String intentAction){
        Intent intent = new Intent(getApplicationContext(), MusicService.class);
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        return new Notification.Action.Builder(icon, title, pendingIntent).build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("LOG", "MusicService: onStartCommand()");
        /*try {
            //Load data from SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            audioList = storage.loadAudio();
            audioIndex = storage.loadAudioIndex();

            if (audioIndex != -1 && audioIndex < audioList.size()) {
                //index is in a valid range
                activeAudio = audioList.get(audioIndex);
            } else {
                stopSelf();
            }
        } catch (NullPointerException e) {
            stopSelf();
        }

        //Request audio focus
        if (requestAudioFocus() == false) {
            //Could not gain focus
            stopSelf();
        }*/

       /* try {
            initMediaSession();
        } catch (RemoteException e) {
            e.printStackTrace();
            stopSelf();
        }
        buildNotification(PlaybackStatus.PAUSED);*/

        //Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent);
        return super.onStartCommand(intent, flags, startId);
    }


}
