package li.lingfeng.ltweaks.xposed;

import android.text.TextUtils;

import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.lib.ZygoteLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.prefs.Prefs;
import li.lingfeng.ltweaks.utils.Logger;

/**
 * Created by smallville on 2016/12/22.
 */

public abstract class Xposed implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    private Set<Class<? extends IXposedHookZygoteInit>> mZygoteModules = new HashSet<>();
    private Set<Class<? extends XposedBase>> mModulesForAll = new HashSet<>(); // These modules are loaded for all packages.
    private Map<String, Set<Class<? extends XposedBase>>> mModules = new HashMap<>();     // package name -> set of Xposed class implemented IXposedHookLoadPackage.
    private Map<Class<?>, Set<String>> mModulePrefs = new HashMap<>(); // Xposed class -> set of enalbed pref.
    private List<IXposedHookLoadPackage> mLoaded = new ArrayList<>();  // Loaded Xposed classes.

    private boolean isEmptyModules() {
        return  mModules.size() == 0;
    }

    protected void addZygoteModule(Class<? extends IXposedHookZygoteInit> cls) {
        mZygoteModules.add(cls);
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

    protected abstract void addZygoteModules();
    protected abstract void addModulesForAll();
    protected abstract void addModules();
    protected abstract void addModulePrefs();

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        File file = new File(Prefs.PATH);
        if (file.exists()) {
            file.setReadable(true, false);
        }
        Prefs.zygotePrefs = new XSharedPreferences(file);
        Prefs.zygotePrefs.makeWorldReadable();

        if (isEmptyModules()) {
            addZygoteModules();
            addModulesForAll();
            addModules();
            addModulePrefs();
        }

        Prefs.useZygotePreferences();
        for (Class<?> cls : mZygoteModules) {
            try {
                ZygoteLoad zygoteLoad = cls.getAnnotation(ZygoteLoad.class);
                List<Integer> enabledPrefs = new ArrayList<>();
                for (int pref : zygoteLoad.prefs()) {
                    if (Prefs.instance().getBoolean(pref, false)) {
                        enabledPrefs.add(pref);
                    }
                }
                if (enabledPrefs.size() > 0 || zygoteLoad.prefs().length == 0) {
                    IXposedHookZygoteInit module = (IXposedHookZygoteInit) cls.newInstance();
                    module.initZygote(startupParam);
                }
            } catch (Throwable e) {
                Logger.e("Can't load zygote module, " + e);
                Logger.stackTrace(e);
            }
        }
        Prefs.clearZygotePreferences();
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        Set<Class<? extends XposedBase>> modules = getModules(lpparam.packageName);
        if (modules == null) {
            return;
        }

        for (Class<?> cls : modules) {
            try {
                XposedLoad xposedLoad = cls.getAnnotation(XposedLoad.class);
                if (xposedLoad.loadPrefsInZygote()) {
                    Prefs.useZygotePreferences();
                }

                List<String> enabledPrefs = new ArrayList<>();
                if (xposedLoad.loadAtActivityCreate().isEmpty()) {
                    Set<String> prefs = getModulePrefs(cls);
                    if (prefs != null) {
                        for (String pref : prefs) {
                            if (Prefs.instance().getBoolean(pref, false)) {
                                enabledPrefs.add(pref);
                            }
                        }
                    }
                }

                if (enabledPrefs.size() > 0 || xposedLoad.prefs().length == 0
                        || !xposedLoad.loadAtActivityCreate().isEmpty()) {
                    if (mModulesForAll.contains(cls)) {
                        if (lpparam.packageName.equals(PackageNames.ANDROID)) {
                            Logger.i("Load " + cls.getName() + " for all packages"
                                    + (xposedLoad.excludedPackages().length == 0 ? "" : (" (exclude " + xposedLoad.excludedPackages().length + ")"))
                                    + ", with prefs [" + TextUtils.join(", ", enabledPrefs) + "]");
                        }
                    }
                    if (xposedLoad.loadAtActivityCreate().isEmpty()) {
                        if (!mModulesForAll.contains(cls)) {
                            Logger.i("Load " + cls.getName() + " for " + lpparam.packageName
                                    + ", with prefs [" + TextUtils.join(", ", enabledPrefs) + "]");
                        }
                    }
                    if (ArrayUtils.contains(xposedLoad.excludedPackages(), lpparam.packageName)) {
                        continue;
                    }
                    IXposedHookLoadPackage module = (IXposedHookLoadPackage) cls.newInstance();
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
