package li.lingfeng.ltweaks.xposed.system;

import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/7/5.
 */
@XposedLoad(packages = PackageNames.ANDROID, prefs = {})
public class XposedTrustAgentWifi extends XposedBase {

    @Override
    protected void handleLoadPackage() throws Throwable {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        hookAllMethods(ClassNames.PACKAGE_MANAGER_SERVICE, "checkPermission", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String packageName = (String) param.args[1];
                String permissionName = (String) param.args[0];
                if (packageName.equals(PackageNames.L_TWEAKS)
                        && permissionName.equals("android.permission.PROVIDE_TRUST_AGENT")) {
                    Logger.i("Grant permission " + permissionName + " for " + packageName);
                    param.setResult(PackageManager.PERMISSION_GRANTED);
                }
            }
        });
    }
}
