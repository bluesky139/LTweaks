package li.lingfeng.ltweaks.xposed.system;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.Prefs;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/3/26.
 */

public abstract class XposedPreventRunning extends XposedBase {

    protected Set<String> mPreventList;
    protected Set<Integer> mPreventUids = new HashSet<>();

    protected abstract int getPreventListKey();

    @Override
    protected void handleLoadPackage() throws Throwable {
        mPreventList = Prefs.instance().getStringSet(getPreventListKey(), new HashSet<String>());
        for (String line : mPreventList) {
            Logger.d("Prevent list item: " + line);
        }

        findAndHookMethod(ClassNames.ACTIVITY_MANAGER_SERVICE, "finishBooting", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (mPreventUids.size() > 0) {
                    return;
                }
                Field field = param.thisObject.getClass().getDeclaredField("mContext");
                field.setAccessible(true);
                Context context = (Context) field.get(param.thisObject);

                for (String line : mPreventList) {
                    try {
                        ApplicationInfo info = context.getPackageManager().getApplicationInfo(line, PackageManager.GET_META_DATA);
                        mPreventUids.add(info.uid);
                        Logger.d("Prevent list item uid: " + info.uid);
                    } catch (Exception e)
                    {}
                }
            }
        });
    }
}
