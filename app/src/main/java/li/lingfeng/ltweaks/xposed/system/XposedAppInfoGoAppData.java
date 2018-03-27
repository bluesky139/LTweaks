package li.lingfeng.ltweaks.xposed.system;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Pair;
import android.widget.Toast;

import java.io.File;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;

/**
 * Created by lilingfeng on 2017/8/15.
 */
@XposedLoad(packages = PackageNames.ANDROID_SETTINGS,
            prefs = R.string.key_app_info_open_app_data_folder,
            loadPrefsInZygote = true)
public class XposedAppInfoGoAppData extends XposedAppInfo {

    private static final String MENU_APP_DATA_FOLDER = "Open App Data Folder";
    private static final String MENU_APP_EXTERNAL_DATA_FOLDER = "Open App External Data Folder";
    private static final String MENU_APP_DEVICE_ENCRYPTED_STORAGE = "Open Device Encrypted Storage";
    private static final String MENU_APP_APK_FOLDER = "Open APK Folder";

    @Override
    protected void handleLoadPackage() throws Throwable {
        super.handleLoadPackage();
        findAndHookMethod(Uri.class, "checkFileUriExposed", String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(null);
            }
        });
    }

    @Override
    protected Pair<String, Integer>[] newMenuNames() {
        return new Pair[] {
                Pair.create(MENU_APP_DATA_FOLDER, 1000),
                Pair.create(MENU_APP_EXTERNAL_DATA_FOLDER, 1001),
                Pair.create(MENU_APP_DEVICE_ENCRYPTED_STORAGE, 1002),
                Pair.create(MENU_APP_APK_FOLDER, 1003)
        };
    }

    @Override
    protected void menuItemSelected(CharSequence menuName, XC_MethodHook.MethodHookParam param) throws Throwable {
        Activity activity = getActivity(param);
        String packageName = getPackageName(param);
        if (MENU_APP_DATA_FOLDER.equals(menuName)) {
            ApplicationInfo info = activity.getPackageManager().getApplicationInfo(packageName, 0);
            ContextUtils.openFolder(activity, info.dataDir);
        } else if (MENU_APP_EXTERNAL_DATA_FOLDER.equals(menuName)) {
            String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + packageName;
            if (new File(dir).exists()) {
                ContextUtils.openFolder(activity, dir);
            } else {
                Toast.makeText(activity, "Folder doesn't exist.", Toast.LENGTH_SHORT).show();
            }
        } else if (MENU_APP_DEVICE_ENCRYPTED_STORAGE.equals(menuName)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                String dir = "/data/user_de/0/" + packageName;
                ContextUtils.openFolder(activity, dir);
            } else {
                Toast.makeText(activity, "Android 7.0+ only.", Toast.LENGTH_SHORT).show();
            }
        } else if (MENU_APP_APK_FOLDER.equals(menuName)) {
            ApplicationInfo info = activity.getPackageManager().getApplicationInfo(packageName, 0);
            ContextUtils.openFolder(activity, new File(info.sourceDir).getParent());
        } else {
            throw new Exception("Unknown menu " + menuName);
        }
    }
}
