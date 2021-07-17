package io.github.ornelasf1.mediaqueue;

import android.content.Context;
import android.view.KeyEvent;
import android.widget.MediaController;

/**
 * Created by Defsin on 5/26/2017.
 */

public class MusicController extends MediaController {
    Context c;
    public MusicController(Context c) {
        super(c);
        this.c = c;
    }

    public void hide(){
    }

    public void hidecontroller(){
        super.hide();
    }
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if(keyCode == KeyEvent.KEYCODE_BACK){
            ((MainActivity)c).onBackPressed();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

}
