package li.lingfeng.ltweaks.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.widget.Toast;

import java.io.Serializable;

import li.lingfeng.ltweaks.MyApplication;
import li.lingfeng.ltweaks.R;

/**
 * Created by smallville on 2017/2/2.
 */

public class PermissionUtils {

    public interface ResultCallback {
        void onResult(boolean ok);
    }

    private static ResultCallback mCallback;

    public static boolean isPermissionGranted(Activity activity, String... permissions) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static boolean tryPermissions(Activity activity, String... permissions) {
        if (!PermissionUtils.isPermissionGranted(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            PermissionUtils.requestPermissions(activity, null, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            Toast.makeText(activity, R.string.app_retry_after_permission_granted, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    public static void requestPermissions(Activity activity, ResultCallback callback, String... permissions) {
        if (isPermissionGranted(activity, permissions)) {
            if (callback != null) {
                callback.onResult(true);
            }
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
        if (mCallback != null) {
            mCallback.onResult(granted);
            mCallback = null;
        }
    }
}
