package cn.jit.musicplayer.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by QZ on 2016/10/22.
 */

public class ToastUtils {
    private static Toast mToast;
    public static void showToast(Context context, int resId, int duration){
        showToast(context, context.getString(resId), duration);
    }
    public static void showToast(Context context, String msg, int duration) {
        if (mToast == null) {
            mToast = Toast.makeText(context, msg, duration);
        } else {
            mToast.setText(msg);
        }
        mToast.show();
    }
}
