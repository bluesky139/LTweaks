package li.lingfeng.ltweaks.utils;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v7.app.AlertDialog;

import li.lingfeng.ltweaks.MyApplication;
import li.lingfeng.ltweaks.R;

/**
 * Created by smallville on 2017/1/12.
 */

public class UninstallUtils {

    public static boolean isPakcageInstalled(String packageName) {
        try {
            MyApplication.instance().getPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return false;
    }

    public static void uninstallPackage(String packageName) {
        Uri uri = Uri.parse("package:" + packageName);
        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MyApplication.instance().startActivity(intent);
    }

    public static void tryUninstallPackage(final String packageName, String appName, Activity activity) {
        if (!isPakcageInstalled(packageName)) {
            return;
        }
        new AlertDialog.Builder(activity)
                .setMessage(activity.getString(R.string.uninstall_message, appName))
                .setPositiveButton(R.string.uninstall_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        uninstallPackage(packageName);
                    }
                })
                .show();
    }
}
