package li.lingfeng.ltweaks.xposed.system;

import android.app.Notification;
import android.content.ComponentName;
import android.os.IBinder;

import java.io.File;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.IOUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/3/22.
 */
@XposedLoad(packages = PackageNames.ANDROID, prefs = R.string.key_prevent_running_prevent_foreground_service)
public class XposedPreventForegroundService extends XposedBase {
    @Override
    protected void handleLoadPackage() throws Throwable {
        final List<String> lines = IOUtils.readLines("/data/system/me.piebridge.prevent.list");
        for (String line : lines) {
            Logger.d("Prevent list item: " + line);
        }

        hookAllMethods(ClassNames.ACTIVITY_MANAGER_SERVICE, "setServiceForeground", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                ComponentName className = (ComponentName) param.args[0];
                if (!lines.contains(className.getPackageName())) {
                    return;
                }
                Logger.i("Prevent foreground service " + className.getClassName());
                param.setResult(null);
            }
        });
    }
}
