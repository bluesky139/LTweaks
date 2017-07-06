package li.lingfeng.ltweaks.xposed.google;

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
public class XposedWifiTrustAgent extends XposedBase {

    @Override
    protected void handleLoadPackage() throws Throwable {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        findAndHookMethod(ClassNames.PACKAGE_MANAGER_SERVICE, "checkPermission", String.class, String.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String packageName = (String) param.args[1];
                String permissionName = (String) param.args[0];
                if (packageName.equals(PackageNames.L_TWEAKS)
                        && (permissionName.equals("android.permission.PROVIDE_TRUST_AGENT")
                        || permissionName.equals("android.permission.CONTROL_KEYGUARD"))) {
                    Logger.i("Grant permission " + permissionName + " for " + packageName);
                    param.setResult(PackageManager.PERMISSION_GRANTED);
                }
            }
        });
    }
}
