package li.lingfeng.ltweaks.xposed.system;

import android.app.Activity;
import android.content.pm.ApplicationInfo;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;

/**
 * Created by lilingfeng on 2017/8/15.
 */
@XposedLoad(packages = PackageNames.ANDROID_SETTINGS, prefs = R.string.key_app_info_open_app_data_folder)
public class XposedAppInfoGoAppData extends XposedAppInfo {

    @Override
    protected String newMenuName() {
        return "Open App Data Folder";
    }

    @Override
    protected void menuItemSelected(Activity activity, String packageName) throws Throwable {
        ApplicationInfo info = activity.getPackageManager().getApplicationInfo(packageName, 0);
        ContextUtils.openFolder(activity, info.dataDir);
    }
}
