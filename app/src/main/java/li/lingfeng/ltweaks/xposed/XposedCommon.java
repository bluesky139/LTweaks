package li.lingfeng.ltweaks.xposed;

import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageParser;

import java.lang.reflect.Field;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;

/**
 * Created by lilingfeng on 2017/6/30.
 */

public abstract class XposedCommon extends XposedBase {

    protected void hookAndSetComponentExported(final String packageName, final String componentName) {
        hookAllMethods(ClassNames.PACKAGE_PARSER, "parseActivity", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                PackageParser.Package owner = (PackageParser.Package) param.args[0];
                if (!owner.packageName.equals(packageName)) {
                    return;
                }

                PackageParser.Activity activity = (PackageParser.Activity) param.getResult();
                Field fieldInfo = PackageParser.Activity.class.getDeclaredField("info");
                fieldInfo.setAccessible(true);
                ActivityInfo info = (ActivityInfo) fieldInfo.get(activity);

                if (!info.name.equals(componentName)) {
                    return;
                }

                Logger.i("Set " + componentName + " exported to true.");
                info.exported = true;
                info.launchMode = ActivityInfo.LAUNCH_MULTIPLE;
            }
        });
    }

    protected void hookAndSetAppDebuggable(final String packageName) {
        hookAllMethods(ClassNames.PACKAGE_PARSER, "parseBaseApplication", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                PackageParser.Package owner = (PackageParser.Package) param.args[0];
                if (!owner.packageName.equals(packageName)) {
                    return;
                }

                Logger.i("Set " + packageName + " debuggable.");
                ApplicationInfo appInfo = owner.applicationInfo;
                appInfo.flags |= ApplicationInfo.FLAG_DEBUGGABLE;
            }
        });
    }
}
