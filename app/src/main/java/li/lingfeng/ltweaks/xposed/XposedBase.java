package li.lingfeng.ltweaks.xposed;

import android.app.Activity;
import android.app.Application;
import android.app.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
            findAndHookMethod(getApplicationClass(), "onCreate", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    MyApplication.setInstanceFromXposed((Application) param.thisObject);
                }
            });
        }
        handleLoadPackage();
    }

    // Application.onCreate() is not called in some app.
    protected Class getApplicationClass() {
        return Application.class;
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
        return findAndHookWithParent(className, Activity.class, methodName, parameterTypesAndCallback);
    }

    protected XC_MethodHook.Unhook findAndHookService(final String className, String methodName, Object... parameterTypesAndCallback) {
        return findAndHookWithParent(className, Service.class, methodName, parameterTypesAndCallback);
    }

    protected XC_MethodHook.Unhook findAndHookWithParent(final String className, final Class clsBase, String methodName, Object... parameterTypesAndCallback) {
        if(parameterTypesAndCallback.length != 0 && parameterTypesAndCallback[parameterTypesAndCallback.length - 1] instanceof XC_MethodHook) {
            Class<?>[] parameterTypes = new Class<?>[parameterTypesAndCallback.length - 1];
            System.arraycopy(parameterTypesAndCallback, 0, parameterTypes, 0, parameterTypes.length);

            // If method is overridden by extended class, then hook it directly.
            Class<?> clsExtended = null;
            try {
                clsExtended = XposedHelpers.findClass(className, lpparam.classLoader);
                clsExtended.getDeclaredMethod(methodName, parameterTypes);
                Logger.v("Hook " + className + " " + methodName);
                return findAndHookMethod(clsExtended, methodName, parameterTypesAndCallback);
            } catch (Throwable e) {}

            // Try find from parent class.
            while (clsExtended != null && clsExtended != Object.class) {
                clsExtended = clsExtended.getSuperclass();
                if (clsExtended == clsBase) {
                    break;
                }
                if (Modifier.isAbstract(clsExtended.getModifiers())) {
                    continue;
                }
                try {
                    clsExtended.getDeclaredMethod(methodName, parameterTypes);
                    break;
                } catch (NoSuchMethodException e) {}
            }
            if (clsExtended == null || clsExtended == Object.class) {
                clsExtended = clsBase;
            }

            // Hook parent class or clsBase.
            final XC_MethodHook hook = (XC_MethodHook) parameterTypesAndCallback[parameterTypesAndCallback.length - 1];
            XC_MethodHook middleHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.thisObject.getClass().getName().equals(className)) {
                        try {
                            XposedHelpers.callMethod(hook, "beforeHookedMethod", param);
                        } catch (XposedHelpers.InvocationTargetError e) {
                            throw e.getCause();
                        }
                    }
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.thisObject.getClass().getName().equals(className)) {
                        try {
                            XposedHelpers.callMethod(hook, "afterHookedMethod", param);
                        } catch (XposedHelpers.InvocationTargetError e) {
                            throw e.getCause();
                        }
                    }
                }
            };
            parameterTypesAndCallback[parameterTypesAndCallback.length - 1] = middleHook;
            Logger.v("Hook " + clsExtended.getName() + " " + methodName + " for " + className);
            return findAndHookMethod(clsExtended, methodName, parameterTypesAndCallback);
        } else {
            throw new IllegalArgumentException("no callback defined");
        }
    }
}
