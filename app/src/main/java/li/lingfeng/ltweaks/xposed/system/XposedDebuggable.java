package li.lingfeng.ltweaks.xposed.system;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageParser;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

@XposedLoad(packages = PackageNames.ANDROID, prefs = R.string.key_debug_debuggable)
public class XposedDebuggable extends XposedBase {
    @Override
    protected void handleLoadPackage() throws Throwable {
        hookAllMethods(ClassNames.PACKAGE_PARSER, "parsePackage", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                PackageParser.Package pkg = (PackageParser.Package) param.getResult();
                if (pkg == null) {
                    return;
                }
                if (pkg.packageName.equals(PackageNames.ANDROID)) {
                    Logger.i("Set debuggable for all apps.");
                    return;
                }

                ApplicationInfo appInfo = pkg.applicationInfo;
                appInfo.flags |= ApplicationInfo.FLAG_DEBUGGABLE;
            }
        });
    }
}
