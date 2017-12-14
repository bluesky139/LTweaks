package li.lingfeng.ltweaks.xposed.system;

import android.content.Intent;
import android.net.Uri;
import android.util.Pair;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;

/**
 * Created by lilingfeng on 2017/12/14.
 */
@XposedLoad(packages = PackageNames.ANDROID_SETTINGS,
        prefs = R.string.key_app_info_app_ops,
        loadPrefsInZygote = true)
public class XposedAppInfoAppOps extends XposedAppInfo {

    private static final String ACTION_APP_OPS_DETAILS = "android.settings.APP_OPS_DETAILS_SETTINGS";
    private static final String APP_OPS_DETAILS = "com.android.settings.applications.AppOpsDetails";

    @Override
    protected Pair<String, Integer>[] newMenuNames() {
        return new Pair[] { Pair.create("App Ops", 1015) };
    }

    @Override
    protected void menuItemSelected(CharSequence menuName, XC_MethodHook.MethodHookParam param) throws Throwable {
        Intent intent = new Intent(ACTION_APP_OPS_DETAILS);
        intent.setClassName(PackageNames.ANDROID_SETTINGS, APP_OPS_DETAILS);
        intent.setData(Uri.parse("package:" + getPackageName(param)));
        getActivity(param).startActivity(intent);
    }
}
