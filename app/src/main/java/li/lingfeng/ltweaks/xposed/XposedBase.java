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
        if (!lpparam.packageName.equals(PackageNames.ANDROID_SYSTEM_UI)
                && !lpparam.packageName.equals(PackageNames.ANDROID)) {
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

    protected XC_MethodHook.Unhook findAndHookMethodByParameterAndReturnTypes(String cls, Class<?> returnType, Object... parameterTypesAndCallback) {
        return findAndHookMethodByParameterAndReturnTypes(findClass(cls), returnType, parameterTypesAndCallback);
    }

    protected XC_MethodHook.Unhook findAndHookMethodByParameterAndReturnTypes(Class<?> cls, Class<?> returnType, Object... parameterTypesAndCallback) {
        if(parameterTypesAndCallback.length != 0 && parameterTypesAndCallback[parameterTypesAndCallback.length - 1] instanceof XC_MethodHook) {
            Class<?>[] parameterTypes = new Class<?>[parameterTypesAndCallback.length - 1];
            System.arraycopy(parameterTypesAndCallback, 0, parameterTypes, 0, parameterTypes.length);
            Method[] methods = XposedHelpers.findMethodsByExactParameters(cls, returnType, parameterTypes);
            if (methods.length == 1) {
                Logger.v("Hook P&R method " + methods[0]);
                return XposedBridge.hookMethod(methods[0], (XC_MethodHook) parameterTypesAndCallback[parameterTypesAndCallback.length - 1]);
            } else {
                for (Method method : methods) {
                    Logger.e("P&R method " + method);
                }
                throw new AssertionError("Can't hook P&R method in cls " + cls + ", " + methods.length + " methods.");
            }
        } else {
            throw new IllegalArgumentException("no callback defined");
        }
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
        if (clsBase == null) {
            throw new AssertionError("clsBase is null.");
        }

        if(parameterTypesAndCallback.length != 0 && parameterTypesAndCallback[parameterTypesAndCallback.length - 1] instanceof XC_MethodHook) {
            Class<?>[] parameterTypes = new Class<?>[parameterTypesAndCallback.length - 1];
            System.arraycopy(parameterTypesAndCallback, 0, parameterTypes, 0, parameterTypes.length);

            // If method is overridden by extended class, then hook it directly.
            Class<?> cls = null;
            Method method = null;
            try {
                cls = findClass(className);
                if (!clsBase.isAssignableFrom(cls)) {
                    throw new AssertionError("Parent of cls " + cls + " is not " + clsBase);
                }
                method = XposedHelpers.findMethodExact(cls, methodName, parameterTypes);
                Logger.v("Hook " + className + " " + methodName);
                return XposedBridge.hookMethod(method, (XC_MethodHook) parameterTypesAndCallback[parameterTypes.length]);
            } catch (AssertionError e) {
                throw e;
            } catch (Throwable e) {
                method = null;
            }

            // Try find from parent class.
            if (cls == null) {
                cls = clsBase;
            }
            while (true) {
                if (cls != clsBase) {
                    cls = cls.getSuperclass();
                }
                try {
                    method = XposedHelpers.findMethodExact(cls, methodName, parameterTypes);
                    if (Modifier.isAbstract(method.getModifiers()) || cls.isInterface()) {
                        throw new Exception();
                    }
                    break;
                } catch (Throwable e) {
                    method = null;
                }
                if (cls == clsBase) {
                    break;
                }
            }
            if (method == null) {
                throw new AssertionError("Method " + methodName + " even can't be found in clsBase " + clsBase
                        + ", or it's abstract.");
            }

            // Hook parent class or clsBase.
            final XC_MethodHook hook = (XC_MethodHook) parameterTypesAndCallback[parameterTypes.length];
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
            Logger.v("Hook " + cls.getName() + " " + methodName + " for " + className);
            return XposedBridge.hookMethod(method, middleHook);
        } else {
            throw new IllegalArgumentException("no callback defined");
        }
    }

    protected Method findMethodStartsWith(String clsName, String methodNameStarts) {
        return findMethodStartsWith(findClass(clsName), methodNameStarts);
    }

    protected Method findMethodStartsWith(Class cls, String methodNameStarts) {
        Method[] methods = cls.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().startsWith(methodNameStarts)) {
                return method;
            }
        }
        return null;
    }
}
