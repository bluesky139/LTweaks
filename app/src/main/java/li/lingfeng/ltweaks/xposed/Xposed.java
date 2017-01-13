package li.lingfeng.ltweaks.xposed;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.prefs.Prefs;

/**
 * Created by smallville on 2016/12/22.
 */

public abstract class Xposed implements IXposedHookLoadPackage {

    private Map<String, Set<Class<?>>> mModules = new HashMap<>();     // package name -> set of Xposed class implemented IXposedHookLoadPackage.
    private Map<Class<?>, Set<String>> mModulePrefs = new HashMap<>(); // Xposed class -> set of enalbed pref.
    private List<IXposedHookLoadPackage> mLoaded = new ArrayList<>();  // Loaded Xposed classes.

    private boolean isEmptyModules() {
        return  mModules.size() == 0;
    }

    protected void addModule(String packageName, Class<?> cls) {
        if (!mModules.containsKey(packageName)) {
            mModules.put(packageName, new HashSet<Class<?>>());
        }
        mModules.get(packageName).add(cls);
    }

    private Set<Class<?>> getModules(String packageName) {
        if (mModules.containsKey(packageName)) {
            return mModules.get(packageName);
        }
        return null;
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

    protected abstract void addModules();
    protected abstract void addModulePrefs();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (isEmptyModules()) {
            addModules();
            addModulePrefs();
        }

        Set<Class<?>> modules = getModules(lpparam.packageName);
        if (modules == null) {
            return;
        }

        for (Class<?> cls : modules) {
            try {
                List<String> enabledPrefs = new ArrayList<>();
                for (String pref : getModulePrefs(cls)) {
                    if (Prefs.instance().getBoolean(pref, false)) {
                        enabledPrefs.add(pref);
                    }
                }

                if (enabledPrefs.size() > 0) {
                    IXposedHookLoadPackage module = (IXposedHookLoadPackage) cls.newInstance();
                    Logger.i("Load " + cls.getName() + " for " + lpparam.packageName
                            + ", with prefs [" + TextUtils.join(", ", enabledPrefs) + "]");
                    module.handleLoadPackage(lpparam);
                    mLoaded.add(module);
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }
}
