package li.lingfeng.ltweaks.xposed;

import android.app.Application;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import li.lingfeng.ltweaks.MyApplication;
import li.lingfeng.ltweaks.prefs.PackageNames;

/**
 * Created by smallville on 2017/1/23.
 */

public abstract class XposedBase implements IXposedHookLoadPackage {

    protected XC_LoadPackage.LoadPackageParam lpparam;

    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        this.lpparam = lpparam;
        if (!lpparam.packageName.equals(PackageNames.ANDROID_SYSTEM_UI)) {
            findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    MyApplication.setInstanceFromXposed((Application) param.thisObject);
                }
            });
        }
        handleLoadPackage();
    }

    protected abstract void handleLoadPackage() throws Throwable;

    protected XC_MethodHook.Unhook findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        return XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
    }

    protected XC_MethodHook.Unhook findAndHookMethod(String className, String methodName, Object... parameterTypesAndCallback) {
        return XposedHelpers.findAndHookMethod(className, lpparam.classLoader, methodName, parameterTypesAndCallback);
    }

    protected XC_MethodHook.Unhook findAndHookConstructor(String className, Object... parameterTypesAndCallback) {
        return XposedHelpers.findAndHookConstructor(className, lpparam.classLoader, parameterTypesAndCallback);
    }

    protected XC_MethodHook.Unhook findAndHookConstructor(Class<?> clazz, Object... parameterTypesAndCallback) {
        return XposedHelpers.findAndHookConstructor(clazz, parameterTypesAndCallback);
    }
}
