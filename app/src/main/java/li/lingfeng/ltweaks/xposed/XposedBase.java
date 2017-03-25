package li.lingfeng.ltweaks.xposed;

import android.app.Activity;
import android.app.Application;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import li.lingfeng.ltweaks.MyApplication;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;

/**
 * Created by smallville on 2017/1/23.
 */

public abstract class XposedBase implements IXposedHookLoadPackage {

    private static Method sMethodBeforeHooked;
    private static Method sMethodAfterHooked;

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

    protected Class<?> findClass(String name) {
        return XposedHelpers.findClass(name, lpparam.classLoader);
    }

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

    protected Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> hookClass, String methodName, XC_MethodHook callback) {
        return XposedBridge.hookAllMethods(hookClass, methodName, callback);
    }

    protected Set<XC_MethodHook.Unhook> hookAllMethods(String className, String methodName, XC_MethodHook callback) {
        return XposedBridge.hookAllMethods(findClass(className), methodName, callback);
    }

    protected Set<XC_MethodHook.Unhook> hookAllConstructors(Class<?> hookClass, XC_MethodHook callback) {
        return XposedBridge.hookAllConstructors(hookClass, callback);
    }

    protected Set<XC_MethodHook.Unhook> hookAllConstructors(String className, XC_MethodHook callback) {
        return XposedBridge.hookAllConstructors(findClass(className), callback);
    }

    protected XC_MethodHook.Unhook findAndHookActivity(final String className, String methodName, Object... parameterTypesAndCallback) {
        if(parameterTypesAndCallback.length != 0 && parameterTypesAndCallback[parameterTypesAndCallback.length - 1] instanceof XC_MethodHook) {
            Class<?>[] parameterTypes = new Class<?>[parameterTypesAndCallback.length - 1];
            System.arraycopy(parameterTypesAndCallback, 0, parameterTypes, 0, parameterTypes.length);

            // If method is override by extended activity, then hook it directly.
            try {
                Class<?> clsActivity = XposedHelpers.findClass(className, lpparam.classLoader);
                clsActivity.getDeclaredMethod(methodName, parameterTypes);
                Logger.v("Hook " + className + " " + methodName);
                return findAndHookMethod(clsActivity, methodName, parameterTypesAndCallback);
            } catch (Throwable e) {}

            // If method is not override by extended activity, hook android.app.Activity.
            final XC_MethodHook hook = (XC_MethodHook) parameterTypesAndCallback[parameterTypesAndCallback.length - 1];
            XC_MethodHook middleHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.thisObject.getClass().getName().equals(className)) {
                        try {
                            getMethodBeforeHooked().invoke(hook, param);
                        } catch (InvocationTargetException e) {
                            Logger.e("Hook activity error in beforeHookedMethod, " + e.getMessage());
                            throw new Exception(e.getCause());
                        }
                    }
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.thisObject.getClass().getName().equals(className)) {
                        try {
                            getMethodAfterHooked().invoke(hook, param);
                        } catch (InvocationTargetException e) {
                            Logger.e("Hook activity error in afterHookedMethod, " + e.getMessage());
                            throw new Exception(e.getCause());
                        }
                    }
                }
            };
            parameterTypesAndCallback[parameterTypesAndCallback.length - 1] = middleHook;
            Logger.v("Hook android.app.Activity " + methodName + " for " + className);
            return findAndHookMethod(Activity.class, methodName, parameterTypesAndCallback);
        } else {
            throw new IllegalArgumentException("no callback defined");
        }
    }

    private Method getMethodBeforeHooked() {
        if (sMethodBeforeHooked == null) {
            try {
                sMethodBeforeHooked = XC_MethodHook.class.getDeclaredMethod("beforeHookedMethod",
                        XC_MethodHook.MethodHookParam.class);
                sMethodBeforeHooked.setAccessible(true);
            } catch (NoSuchMethodException e) {
                Logger.e("Can't get method beforeHookedMethod");
                e.printStackTrace();
            }
        }
        return sMethodBeforeHooked;
    }

    private Method getMethodAfterHooked() {
        if (sMethodAfterHooked == null) {
            try {
                sMethodAfterHooked = XC_MethodHook.class.getDeclaredMethod("afterHookedMethod",
                        XC_MethodHook.MethodHookParam.class);
                sMethodAfterHooked.setAccessible(true);
            } catch (NoSuchMethodException e) {
                Logger.e("Can't get method afterHookedMethod");
                e.printStackTrace();
            }
        }
        return sMethodAfterHooked;
    }
}
