package io.github.ornelasf1.mediaqueue;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Checkable;

import java.util.ArrayList;

/**
 * Created by Defsin on 6/4/2017.
 */

public class SongCheckable implements Checkable {
    private boolean mChecked;
    private View mView;
    private Context mContext;
    private int songPos;

    public SongCheckable(){
    }

    public SongCheckable(Context context, LinearLayout view){
        mContext = context;
        mView = view;
    }

    public void highlightView(int pos){
        /*songViewList.get(pos).setBackgroundColor(mContext.getResources().getColor(R.color.colorWhite));*/
    }

    public void setPosition(int pos){
        songPos = pos;
    };

    @Override
    public void setChecked(boolean checked) {
        mChecked = checked;
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void toggle() {
        if(!mChecked){
            mChecked = true;
        }else{
            mChecked = false;
        }
    }
}
