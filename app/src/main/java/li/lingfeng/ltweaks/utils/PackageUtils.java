package li.lingfeng.ltweaks.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Process;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.MyApplication;
import li.lingfeng.ltweaks.R;

/**
 * Created by smallville on 2017/1/12.
 */

public class PackageUtils {

    public static final int SORT_BY_NAME = 0;
    public static final int SORT_BY_DATE = 1;

    public static List<PackageInfo> getInstalledPackages() {
        return MyApplication.instance().getPackageManager().getInstalledPackages(0);
    }

    public static void sortPackages(List<PackageInfo> packages, final int sort) {
        final PackageManager packageManager = MyApplication.instance().getPackageManager();
        Collections.sort(packages, new Comparator<PackageInfo>() {
            @Override
            public int compare(PackageInfo o1, PackageInfo o2) {
                if (sort == SORT_BY_NAME) {
                    return o1.applicationInfo.loadLabel(packageManager).toString().compareTo(
                            o2.applicationInfo.loadLabel(packageManager).toString());
                } else if (sort == SORT_BY_DATE) {
                    return (int) (o1.firstInstallTime - o2.firstInstallTime);
                } else  {
                    throw new RuntimeException("sortPackages() unknown sort " + sort);
                }
            }
        });
    }

    public static boolean isPackageInstalled(String packageName) {
        try {
            MyApplication.instance().getPackageManager().getPackageInfo(packageName, 0);
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
        if (!isPackageInstalled(packageName)) {
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

    public static boolean killPackage(Context context, String packageName) throws Throwable {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ApplicationInfo info = context.getPackageManager().getApplicationInfo(packageName, 0);
        if (info.uid >= Process.FIRST_APPLICATION_UID && info.uid <= Process.LAST_APPLICATION_UID) {
            Logger.i("Kill " + packageName);
            XposedHelpers.callMethod(activityManager, "forceStopPackage", packageName);

            CharSequence label = info.loadLabel(context.getPackageManager());
            String toastStr = ContextUtils.getLString(R.string.app_kill_hint);
            toastStr = String.format(toastStr, label);
            Toast.makeText(context, toastStr, Toast.LENGTH_SHORT).show();
            return true;
        } else {
            Logger.i("Should not kill system app " + packageName);
            return false;
        }
    }
}
