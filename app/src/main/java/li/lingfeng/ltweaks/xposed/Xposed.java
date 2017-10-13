package li.lingfeng.ltweaks.xposed;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.crossbowffs.remotepreferences.RemotePreferences;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.prefs.Prefs;

/**
 * Created by smallville on 2016/12/22.
 */

public abstract class Xposed implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    private Set<Class<? extends XposedBase>> mModulesForAll = new HashSet<>(); // These modules are loaded for all packages.
    private Map<String, Set<Class<? extends XposedBase>>> mModules = new HashMap<>();     // package name -> set of Xposed class implemented IXposedHookLoadPackage.
    private Map<Class<?>, Set<String>> mModulePrefs = new HashMap<>(); // Xposed class -> set of enalbed pref.
    private List<IXposedHookLoadPackage> mLoaded = new ArrayList<>();  // Loaded Xposed classes.

    private boolean isEmptyModules() {
        return  mModules.size() == 0;
    }

    protected void addModuleForAll(Class<? extends XposedBase> cls) {
        mModulesForAll.add(cls);
    }

    protected void addModule(String packageName, Class<? extends XposedBase> cls) {
        if (!mModules.containsKey(packageName)) {
            mModules.put(packageName, new HashSet<Class<? extends XposedBase>>());
        }
        mModules.get(packageName).add(cls);
    }

    private Set<Class<? extends XposedBase>> getModules(String packageName) {
        Set<Class<? extends XposedBase>> modules = new HashSet<>(mModulesForAll);
        if (mModules.containsKey(packageName)) {
            modules.addAll(mModules.get(packageName));
        }
        return modules;
    }

    protected void addModulePref(Class<?> cls, String pref) {
        if (!mModulePrefs.containsKey(cls)) {
            mModulePrefs.put(cls, new HashSet<String>());
        }
        mModulePrefs.get(cls).add(pref);
    }

    private Set<String> getModulePrefs(Class<?> cls) {
        return mModulePrefs.get(cls);
    }

    protected abstract void addModulesForAll();
    protected abstract void addModules();
    protected abstract void addModulePrefs();

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        XSharedPreferences pref = new XSharedPreferences(PackageNames.L_TWEAKS);
        pref.makeWorldReadable();
        Prefs.xprefs = pref;
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (isEmptyModules()) {
            addModulesForAll();
            addModules();
            addModulePrefs();
        }

        // Use remote preferences for com.android.settings, to fix reading preference denied by SELinux
        if (!ArrayUtils.contains(PackageNames._SYSTEM_BOOT_PACKAGES, lpparam.packageName)) {
            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Application app = (Application) param.thisObject;
                    RemotePreferences pref = new RemotePreferences(app,
                            "li.lingfeng.ltweaks.mainpreferences", "li.lingfeng.ltweaks_preferences");
                    Prefs.xprefs = pref;
                    handleLoadPackageByPrefs(lpparam, pref);
                }
            });
        } else {
            handleLoadPackageByPrefs(lpparam, Prefs.instance());
        }
    }

    private void handleLoadPackageByPrefs(XC_LoadPackage.LoadPackageParam lpparam,
                                          SharedPreferences sharedPreferences) {
        Set<Class<? extends XposedBase>> modules = getModules(lpparam.packageName);
        if (modules == null) {
            return;
        }

        for (Class<?> cls : modules) {
            try {
                List<String> enabledPrefs = new ArrayList<>();
                Set<String> prefs = getModulePrefs(cls);
                if (prefs != null) {
                    for (String pref : prefs) {
                        if (sharedPreferences.getBoolean(pref, false)) {
                            enabledPrefs.add(pref);
                        }
                    }
                }

                if (enabledPrefs.size() > 0 || cls.getAnnotation(XposedLoad.class).prefs().length == 0) {
                    IXposedHookLoadPackage module = (IXposedHookLoadPackage) cls.newInstance();
                    if (mModulesForAll.contains(cls)) {
                        if (lpparam.packageName.equals(PackageNames.ANDROID)) {
                            Logger.i("Load " + cls.getName() + " for all packages"
                                    + ", with prefs [" + TextUtils.join(", ", enabledPrefs) + "]");
                        }
                    } else {
                        Logger.i("Load " + cls.getName() + " for " + lpparam.packageName
                                + ", with prefs [" + TextUtils.join(", ", enabledPrefs) + "]");
                    }
                    module.handleLoadPackage(lpparam);
                    mLoaded.add(module);
                }
            } catch (Throwable e) {
                Logger.e("Can't handleLoadPackage, " + e.getMessage());
                Logger.stackTrace(e);
            }
        }
    }
}
