package li.lingfeng.ltweaks.xposed.system;

import android.os.Binder;
import android.os.Build;
import android.os.PowerManager;

import java.lang.reflect.Method;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/3/26.
 */
@XposedLoad(packages = PackageNames.ANDROID, prefs = R.string.key_prevent_running_prevent_wake_lock)
public class XposedPreventWakeLock extends XposedPreventRunning {

    private static final int WAKE_LOCK_LEVEL_MASK = 0x0000ffff;

    @Override
    protected void handleLoadPackage() throws Throwable {
        super.handleLoadPackage();
        hookAllMethods(ClassNames.POWER_MANAGER_SERVICE + "$BinderService", "acquireWakeLock", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                int uid = Binder.getCallingUid();
                if (sPreventUids.contains(uid)) {
                    int pos = 1;
                    if (param.args[0] instanceof Integer) {
                        pos = 0;  // <= JELLY_BEAN
                    }
                    int flags = (int) param.args[pos];
                    if ((flags & WAKE_LOCK_LEVEL_MASK) == PowerManager.PARTIAL_WAKE_LOCK) {
                        param.setResult(null);
                        Logger.d("Prevent acquireWakeLock uid " + uid);
                    }
                }
            }
        });

        hookAllMethods(ClassNames.POWER_MANAGER_SERVICE + "$BinderService", "updateWakeLockWorkSource", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                int uid = Binder.getCallingUid();
                if (sPreventUids.contains(uid)) {
                    param.setResult(null);
                    Logger.d("Prevent updateWakeLockWorkSource uid " + uid);
                }
            }
        });
    }
}
