package io.github.ornelasf1.mediaqueue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Created by Defsin on 5/31/2017.
 */

public class NotificationConstants {
    public interface ACTION {
        public static String ACTION_PLAY = "io.github.ornelasf1.ACTION_PLAY";
        public static String ACTION_PAUSE = "io.github.ornelasf1.ACTION_PAUSE";
        public static String ACTION_PREVIOUS = "io.github.ornelasf1.ACTION_PREVIOUS";
        public static String ACTION_NEXT = "io.github.ornelasf1.ACTION_NEXT";
        public static String ACTION_STOP = "io.github.ornelasf1.ACTION_STOP";
        public static String ACTION_ENTER = "io.github.ornelasf1.ACTION_ENTER";

    }

    public interface NOTIFICATION_ID{
        public static int FOREGROUND_SERVICE = 101;
    }

    public static Bitmap getDefaultAlbumArt(Context context) {
        Bitmap bm = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        try {
            bm = BitmapFactory.decodeResource(context.getResources(),
                    R.mipmap.mediaqueue_logo_green, options);
        } catch (Error ee) {
        } catch (Exception e) {
        }
        return bm;
    }

}
