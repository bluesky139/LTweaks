package li.lingfeng.ltweaks.xposed.system;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
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
            loadAtActivityCreate = XposedAppInfo.SETTINGS_ACTIVITY,
            useRemotePreferences = true)
public class XposedAppInfoGoAppData extends XposedAppInfo {

    private static final String MENU_APP_DATA_FOLDER = "Open App Data Folder";
    private static final String MENU_APP_EXTERNAL_DATA_FOLDER = "Open App External Data Folder";

    @Override
    protected Pair<String, Integer>[] newMenuNames() {
        return new Pair[] {
                Pair.create(MENU_APP_DATA_FOLDER, 1000),
                Pair.create(MENU_APP_EXTERNAL_DATA_FOLDER, 1001)
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
        } else {
            throw new Exception("Unknown menu " + menuName);
        }
    }
}
