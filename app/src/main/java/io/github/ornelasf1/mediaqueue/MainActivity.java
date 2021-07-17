package io.github.ornelasf1.mediaqueue;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;

import quickscroll.QuickScroll;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, MediaController.MediaPlayerControl, BooleanChange.Listener {

    private static final String LT = "LOG";

    private ArrayList<Song> songList;
    private ListView songView;
    private ListView queueSongView;
    private TextView hintMes;
    private LinearLayout songLinearLayout;
    private Button clearButt;
    private SongAdapter songAdt;
    private Queue queueEntry;
    private int lastViewTag;

    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound = false;

    private static MusicController controller;

    private static boolean paused=false, playbackPaused = false;

    public static BooleanChange bChangLis;

    public TextSwitcher songSwitch;
    private ImageView imgLogo;

    static boolean stopOnEndCheck;
    static boolean sortedByTitle;

    private double scrollstore = 0;
    private ImageView scrollBar;

    private final int RequestPermissionCode = 1;

    public static boolean onStartBool = false;

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.d(LT, "MainActivity: onRequestPermissionsResult()");
        switch (requestCode) {
            case RequestPermissionCode:
                if (grantResults.length> 0) {
                    Log.d(LT, "MainActivity: onRequestPermissionsResult: Permission was granted");
                    boolean perm1 = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;

                    if (perm1) {
                        Log.d(LT, "MainActivity: onRequestPermissionsResult: Permission Granted");
                        onCreateInit();
                        onStart();

                    } else {
                        finish();
                        System.exit(0);
                        Log.d(LT, "MainActivity: onRequestPermissionsResult: Permission Denied");
                    }
                }
                break;
        }
    }

    public boolean checkPermission(){
        return ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(LT, "MainActivity: onCreate()");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(LT, "MainActivity: current sdk is GREATER than M: " + Build.VERSION.SDK_INT + " >= " + Build.VERSION_CODES.M);
            if (checkPermission()) {
                Log.d(LT, "onCreate: Permission is granted");
                //TODO Handle when permission is not given (App crashed when the permission dialog was given)
            }else{
                Log.d(LT, "onCreate: Permission not granted | Now requesting");
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, RequestPermissionCode);
            }
        }else{
            Log.d(LT, "MainActivity: current sdk is LOWER than M: " + Build.VERSION.SDK_INT + " >= " + Build.VERSION_CODES.M);
        }

        onCreateInit();
    }

    public void onCreateInit(){
        if(checkPermission()) {
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            });*/

            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            //Hamburger button toggle
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.addDrawerListener(toggle);
            toggle.syncState();

            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            navigationView.setNavigationItemSelectedListener(this);

            setTitle("");

            songView = (ListView) findViewById(R.id.song_list);
            queueSongView = (ListView) findViewById(R.id.queue_list);
            songList = new ArrayList<Song>();
            hintMes = (TextView) findViewById(R.id.hintMessage);
            hintMes.setTypeface(Typeface.createFromAsset(getAssets(), OswaldFont.LIGHT));
            imgLogo = (ImageView) findViewById(R.id.logoImg);
            songLinearLayout = (LinearLayout) findViewById(R.id.song_linear);
            clearButt = (Button) findViewById(R.id.clearQueueButt);
            clearButt.setTypeface(Typeface.createFromAsset(getAssets(), OswaldFont.MEDIUM));

            getSongList();

            sortedByTitle = true;
            Collections.sort(songList, new Comparator<Song>() {
                public int compare(Song a, Song b) {
                    if (a.getTitle().equalsIgnoreCase(b.getTitle())) {
                        return a.getTitle().compareTo(b.getTitle());
                    }
                    return a.getTitle().toUpperCase().compareTo(b.getTitle().toUpperCase());
                }
            });

            songAdt = new SongAdapter(this, songList);
            songView.setAdapter(songAdt);

            setController();

            initNavQueue();
            bChangLis = new BooleanChange();
            bChangLis.registerListener(this);

            songSwitch = (TextSwitcher) findViewById(R.id.currentSongPlaying);

            songSwitch.setFactory(new ViewSwitcher.ViewFactory() {
                public View makeView() {
                    OswaldFont myText = new OswaldFont(MainActivity.this, OswaldFont.REGULAR);
                    myText.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
                    myText.setTextSize(16);
                    myText.setTextColor(Color.parseColor("#ffffff"));
                    return myText;
                }
            });

            setScrollBar();

            /*SwipeDismissListViewTouchListener touchListener =
                    new SwipeDismissListViewTouchListener(queueSongView, new SwipeDismissListViewTouchListener.DismissCallbacks() {
                        @Override
                        public boolean canDismiss(int position) {
                            return true;
                        }

                        @Override
                        public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                            for (int position : reverseSortedPositions) {
                                Log.d(LT, "MainActivity: Removing from list");
                                Queue.songQueue.remove(queueEntry.getItem(position));
                            }
                            queueEntry.notifyDataSetChanged();
                        }
                    });
            queueSongView.setOnTouchListener(touchListener);
            queueSongView.setOnScrollListener(touchListener.makeScrollListener());*/

        }
    }

    public void setScrollBar(){
        Log.d(LT, "MainActivity(): setScrollbar()");
        final QuickScroll quickscroll = (QuickScroll) findViewById(R.id.quickScroll);
        quickscroll.init(QuickScroll.TYPE_INDICATOR_WITH_HANDLE, songView, songAdt, QuickScroll.STYLE_HOLO);
        quickscroll.setFixedSize(1);
        quickscroll.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 36);
        quickscroll.setPopupColor(Color.WHITE, Color.WHITE, 2, Color.DKGRAY, 100);
        quickscroll.setHandlebarColor(Color.WHITE, Color.WHITE, Color.LTGRAY);
        quickscroll.setFontType(Typeface.createFromAsset(getResources().getAssets(), OswaldFont.MEDIUM));
    }

    public void initiateValues(){
        musicSrv.setTextSwitcher(songSwitch);
    }

    public void timedDialog(){
        Handler handler = new Handler();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                setScrollBar();
            }
        };

        handler.postDelayed(runnable, 10000);
    }

    public View getViewByPosition(int pos, ListView listView) {
        try {
            final int firstListItemPosition = listView
                    .getFirstVisiblePosition();
            final int lastListItemPosition = firstListItemPosition
                    + listView.getChildCount() - 1;

            if (pos < firstListItemPosition || pos > lastListItemPosition) {
                //This may occure using Android Monkey, else will work otherwise
                return listView.getAdapter().getView(pos, null, listView);
            } else {
                final int childIndex = pos - firstListItemPosition;
                return listView.getChildAt(childIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public void onSongCompletion(boolean state){
        if(state){
            Log.d(LT, "MainActivity: BooleanChangeListener: onSongCompletion() SUCCESS");
            updateNavView();
            songView.invalidateViews();
            scrollToSelection(musicSrv.getSongPos());
        }else{
            Log.d(LT, "MainActivity: BooleanChangeListener: onSongCompletion() FAILED");
        }
    }

    public void scrollToSelection(int pos){
        if(musicSrv.getShuffle()) {
            int h1 = songView.getHeight();
            songView.setSelection(pos);
            songView.setSelectionFromTop(pos, h1/2);
        }else{
            songView.smoothScrollToPosition(pos);
        }
    }

    public void displayHintMessage(){
        updateNavView();
        songView.invalidateViews();
    }

    public void closeApp(){
        finish();
        System.exit(0);
    }

    @Override
    protected void onStart(){
        super.onStart();
        Log.d(LT, "MainActivity: onStart()");
        if(checkPermission()) {
            if (playIntent == null) {
                Log.d(LT, "playIntent is null");
                playIntent = new Intent(this, MusicService.class);
                bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
                startService(playIntent);
            }
        }
    }

    @Override
    protected void onDestroy(){
        Log.d(LT, "MainActivity: onDestroy()");
        stopService(playIntent);
        musicSrv=null;
        controller.hidecontroller();
        unbindService(musicConnection);
        controller = null;
        super.onDestroy();
    }

    @Override
    protected void onPause(){
        Log.d(LT, "MainActivity: onPause()");
        if(checkPermission()) {
            paused = true;
            controller.hidecontroller();
        }
        super.onPause();
    }

    @Override
    protected void onResume(){
        Log.d(LT, "MainActivity: onResume()");
        super.onResume();
        if(checkPermission()) {
            if (paused) {
                setController();
                paused = false;
            }
            if (musicSrv != null) {
                showController();
            }
        }
    }

    @Override
    protected void onStop(){
        Log.d(LT, "MainActivity: onStop()");
        super.onStop();
    }


    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(LT, "MainActivity: Connection made | ServiceConnection");
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
            musicSrv = binder.getService();
            musicSrv.setList(songList);
            musicSrv.setSongListView(songView);
            initiateValues();
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    public void currentSongTextClicked(View v){
        Log.d(LT, "MainActivity: textSwitcher clicked | scrolling to position: " + musicSrv.getSongPos());
        scrollToSelection(musicSrv.getSongPos());
    }

    public void songPicked(View view){
        Log.d(LT, "MainActivity: songPicked()");
        MusicService.setNextSongPos(musicSrv.getSongPos());
        musicSrv.clearChecksAndHighlight(Integer.parseInt(view.getTag().toString()));
        songView.invalidateViews();

        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();
        if(playbackPaused){
            setController();
            playbackPaused = false;
        }
        if(!Queue.songQueue.isEmpty()){
            Queue.queueActive = true;
        }else{
            Queue.queueActive = false;
        }
        showController();
        imgLogo.setVisibility(View.GONE);


        //displaySongOnToolBar(musicSrv.getSong(Integer.parseInt(view.getTag().toString())).getTitle(), musicSrv.getSong(Integer.parseInt(view.getTag().toString())).getArtist());
    }

    public static boolean getPlaybackStatus(){
        return playbackPaused;
    }

    public static void setPlaybackStatus(boolean b){
        playbackPaused = b;
    }

    public void queuePlay(View view){
        if(!Queue.songQueue.isEmpty()) {
            musicSrv.setSong(Queue.songQueue.get(0).getPos());
            musicSrv.playSong();
            Queue.queueActive = true;
        }else{
            Queue.queueActive = false;
        }
    }

    public void queueClear(View view){
        if(!Queue.songQueue.isEmpty()){
            Queue.songQueue.clear();
            Queue.queueActive = false;
        }
        updateNavView();
    }

    public void getSongList(){
        Log.d(LT, "MainActivity: getSongList()");
        ContentResolver musicResolver = getContentResolver(); //Content providers offer data encapsulation based on URIs; allows apps to use data
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI; //immutable one-to-one mapping to a resource or data
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (musicCursor!= null && musicCursor.moveToFirst()) {  //If query was successful
            int titleColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST);
            do{
                long thisID = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                Song song = new Song(thisID, thisTitle, thisArtist);
                songList.add(song);
            }while(musicCursor.moveToNext());
            musicCursor.close();
        }
    }


    @Override
    public void onBackPressed() {
        Log.d(LT, "MainActivity: Back is pressed");
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            onPause();
            onStop();
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(LT, "OnMenuOptionSelected: StopOneEndCheck = true");
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                if(item.isChecked()){
                    item.setChecked(false);
                }
                else{
                    item.setChecked(true);
                }
                musicSrv.setShuffle();
                break;
            case R.id.action_sortBy:
                if(item.isChecked()) {
                    item.setChecked(false);
                    item.setTitle(getString(R.string.sortby_title));
                }else{
                    item.setChecked(true);
                    item.setTitle(getString(R.string.sortby_artist));
                }
                sortByToggle();
                break;
            case R.id.action_end:
                if(item.isChecked()){
                    item.setChecked(false);
                    stopOnEndCheck = false;
                }else {
                    item.setChecked(true);
                    stopOnEndCheck = true;
                }
                break;
            case R.id.action_settings:
                break;

/*            case R.id.action_end:
                item.setChecked(true);
                stopOnCheck = true;
                if(item.isChecked()){
                }else{
                    stopOnCheck = false;
                }
                break;*/

        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void sortByToggle(){
        if(sortedByTitle){
            sortedByTitle = false;
            Collections.sort(songList, new Comparator<Song>() {
                public int compare(Song a, Song b) {
                    if (a.getArtist().equalsIgnoreCase(b.getArtist())) {
                        return a.getArtist().compareTo(b.getArtist());
                    }
                    return a.getArtist().toUpperCase().compareTo(b.getArtist().toUpperCase());
                }
            });
        }else{
            sortedByTitle = true;
            Collections.sort(songList, new Comparator<Song>() {
                public int compare(Song a, Song b) {
                    if (a.getTitle().equalsIgnoreCase(b.getTitle())) {
                        return a.getTitle().compareTo(b.getTitle());
                    }
                    return a.getTitle().toUpperCase().compareTo(b.getTitle().toUpperCase());
                }
            });
        }
        songView.invalidateViews();
    }

    private void initNavQueue() {
        // Sets each song with a functionality.
        songView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(LT, "long click from main_activity " );
                Toast toast = Toast.makeText(MainActivity.this, musicSrv.getSong(position).getTitle() + " has been added to the Queue.", Toast.LENGTH_SHORT);
                toast.show();
                queueEntry = new Queue(MainActivity.this, musicSrv.getSong(position));
                musicSrv.getSong(position).setPos(position);
                //SongAdapter songAdt = new SongAdapter(MainActivity.this, queueEntry.songQueue);
                queueSongView.setAdapter(queueEntry);
                Queue.queueActive = true;
                MainActivity.bChangLis.notifyIfSongCompleted(false);
                updateNavView();

                return true;
            }
        });

        /*queueSongView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(LT, "Here's on click in Main");
                musicSrv.setSong(position);
                musicSrv.playSong();
            }
        });*/
    }

    public void updateNavView(){
        if(!Queue.songQueue.isEmpty()) {
            Log.d(LT, "Hint message is GONE");
            hintMes.setVisibility(View.GONE);
            for (int i = 0; i < Queue.songQueue.size(); i++) {
                Queue queueEntry = new Queue(MainActivity.this, Queue.songQueue.get(i));
                queueSongView.setAdapter(queueEntry);
            }
        }else{
            Log.d(LT, "Hint message is VISIBLE");
            queueSongView.setAdapter(null);
            hintMes.setVisibility(View.VISIBLE);
        }
    }

    /*private void init() {
        songList = new ArrayList<>();
        songView = (ListView) findViewById(R.id.song_list);

        // Sets each song with a functionality.
        songView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Log.d(LT, "Song item clicked");

                // Sets the respective song in the Service, and then plays it.
                musicSrv.setSong(position);
                musicSrv.playSong();
                // Used to address when the user interacts with the controls while playback is
                // paused since the MediaPlayer object may behave strangely.

                // Sets the flag to false for the controller's duration and position purposes.
                if (playbackPaused) playbackPaused = false;
            }
        });
    }*/

    public static void showController() {
        Log.d(LT, "showController()");
        controller.show(0);
    }

    private void setController(){
        if(controller == null){
            Log.d(LT, "MainActivity: controller = new MusicController");
            controller = new MusicController(this);
        }else{
            Log.d(LT, "MainActivity: controller is not null");
        }
        controller.setBackgroundColor(getResources().getColor(R.color.colorBlack));
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });

        controller.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d(LT, "MainActivity: Here's on touch listener!");
                return false;
            }
        });

        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.mainLayout));
        controller.setEnabled(true);

        Log.d(LT, "Controller set | MainActivity.setController()");
    }

    private void playNext(){
        if (playbackPaused) playbackPaused = false;
        if(!Queue.songQueue.isEmpty()){
            Log.d(LT, "MainActivity: playNext(): If queue is NOT empty, enable queueActive");
            Queue.queueActive = true;
        }else{
            Log.d(LT, "MainActivity: playNext(): If queue is empty, disable queueActive");
            Queue.queueActive = false;
            musicSrv.stopOnEmptyQueue();
        }
        musicSrv.playNext();
        if(imgLogo.getVisibility() == View.VISIBLE) imgLogo.setVisibility(View.GONE);
        updateNavView();
        songView.invalidateViews(); //Redraws listView so that the list can retrieve the new highlighted song
        scrollToSelection(musicSrv.getSongPos());
    }

    private void playPrev(){
        musicSrv.playPrev();
        songView.invalidateViews();
        if (playbackPaused) playbackPaused = false;
        if(!Queue.songQueue.isEmpty()){
            Queue.queueActive = true;
        }else{
            Queue.queueActive = false;
        }
        if(imgLogo.getVisibility() == View.VISIBLE) imgLogo.setVisibility(View.GONE);
        scrollToSelection(musicSrv.getSongPos());
    }

    @Override
    public void start() {
        Log.d(LT, "THIS IS PLAY");
        if(!musicSrv.ifRetrievedAudioFocus()){
            return;
        }
        if(imgLogo.getVisibility() == View.VISIBLE) imgLogo.setVisibility(View.GONE);
        playbackPaused = false;
        musicSrv.go();
    }

    @Override
    public void pause() {
        Log.d(LT, "THIS IS PAUSE");
        if(!musicSrv.ifRetrievedAudioFocus()){
            return;
        }
        playbackPaused = true;
        musicSrv.pausePlayer();
    }

    @Override
    public int getDuration() {
        if(musicSrv!=null && musicBound && onStartBool){
            //Log.d(LT, "MainActivity: Updating duration of song");
            return musicSrv.getDur();
        }else {
            //Log.d(LT, "MainActivity: Setting duration of song to ZERO");
            return 0;
        }
    }

    @Override
    public int getCurrentPosition() {
        if(musicSrv != null && musicBound && onStartBool) {
            //Log.d(LT, "MainActivity: Updating current position of song");
            return musicSrv.getPosn();
        }else{
            //Log.d(LT, "MainActivity: Setting current position of song to ZERO");
            return 0;
        }
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
    }

    @Override
    public boolean isPlaying() {
        if(musicSrv != null && musicBound){
            return musicSrv.isPng();
        }
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

}
