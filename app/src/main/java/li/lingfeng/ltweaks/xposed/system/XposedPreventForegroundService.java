package li.lingfeng.ltweaks.xposed.system;

import android.content.ComponentName;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;

/**
 * Created by smallville on 2017/3/22.
 */
@XposedLoad(packages = PackageNames.ANDROID, prefs = {})
public class XposedPreventForegroundService extends XposedPreventRunning {
    @Override
    protected int getPreventListKey() {
        return R.string.key_prevent_list_prevent_foreground_service;
    }

    @Override
    protected void handleLoadPackage() throws Throwable {
        super.handleLoadPackage();
        hookAllMethods(ClassNames.ACTIVITY_MANAGER_SERVICE, "setServiceForeground", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                ComponentName className = (ComponentName) param.args[0];
                if (!mPreventList.contains(className.getPackageName())) {
                    return;
                }
                Logger.i("Prevent foreground service " + className.getClassName());
                param.setResult(null);
            }
        });
    }
}
