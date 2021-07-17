package io.github.ornelasf1.mediaqueue;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.AppCompatTextView;
import android.widget.TextView;

/**
 * Created by Defsin on 6/16/2017.
 */

public class OswaldFont extends AppCompatTextView {
    private Context mContext;
    static final String BOLD = "Oswald-Bold.ttf";
    static final String EXTRALIGHT = "Oswald-ExtraLight.ttf";
    static final String LIGHT = "Oswald-Light.ttf";
    static final String MEDIUM = "Oswald-Medium.ttf";
    static final String REGULAR = "Oswald-Regular.ttf";
    static final String SEMIBOLD = "Oswald-SemiBold.ttf";
    private Typeface tf;

    public OswaldFont(Context context) {
        super(context);
    }

    public OswaldFont(Context context, String fontType) {
        super(context);
        mContext = context;
        tf = Typeface.createFromAsset(mContext.getAssets(), fontType);
        init();
    }

    private void init() {
        setTypeface(tf);
    }
}
