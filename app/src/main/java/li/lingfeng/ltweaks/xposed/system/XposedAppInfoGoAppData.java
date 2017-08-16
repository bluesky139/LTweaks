package li.lingfeng.ltweaks.xposed.system;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.util.Pair;

import de.robv.android.xposed.XC_MethodHook;
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
    protected Pair<String, Integer>[] newMenuNames() {
        return new Pair[] { Pair.create("Open App Data Folder", 1000) };
    }

    @Override
    protected void menuItemSelected(CharSequence menuName, XC_MethodHook.MethodHookParam param) throws Throwable {
        Activity activity = getActivity(param);
        String packageName = getPackageName(param);
        ApplicationInfo info = activity.getPackageManager().getApplicationInfo(packageName, 0);
        ContextUtils.openFolder(activity, info.dataDir);
    }
}
