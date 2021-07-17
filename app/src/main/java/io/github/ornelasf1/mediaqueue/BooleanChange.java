package io.github.ornelasf1.mediaqueue;

/**
 * Created by Defsin on 5/30/2017.
 */

public class BooleanChange {

    public interface Listener{
        void onSongCompletion(boolean state);
        void closeApp();
        void displayHintMessage();
    }

    private Listener mListener = null;

    public void registerListener(Listener listener){
        mListener = listener;
    }

    private boolean isSongCompleted = false;
    private boolean closeApp = false;

    public void notifyIfSongCompleted(boolean state){

        isSongCompleted = state;

        if(mListener != null){
            mListener.onSongCompletion(isSongCompleted);
        }
    }

    public void updateNavView(){
        mListener.displayHintMessage();
    }

    public void closeAppOnNotification(){
        mListener.closeApp();
    }
}
