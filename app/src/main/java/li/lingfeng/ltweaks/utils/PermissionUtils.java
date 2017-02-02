package li.lingfeng.ltweaks.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import java.io.Serializable;

import li.lingfeng.ltweaks.MyApplication;

/**
 * Created by smallville on 2017/2/2.
 */

public class PermissionUtils {

    public interface ResultCallback {
        void onResult(boolean ok);
    }

    private static ResultCallback mCallback;

    public static void requestPermissions(Activity activity, ResultCallback callback, String... permissions) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            callback.onResult(true);
            return;
        }

        boolean needGrant = false;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(MyApplication.instance(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                needGrant = true;
                break;
            }
        }

        if (!needGrant) {
            callback.onResult(true);
            return;
        }
        mCallback = callback;
        ActivityCompat.requestPermissions(activity, permissions, 0);
    }

    public static void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        boolean granted = true;
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                granted = false;
                break;
            }
        }
        Logger.i("Permissions are granted = " + granted);
        mCallback.onResult(granted);
        mCallback = null;
    }
}
