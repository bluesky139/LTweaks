package li.lingfeng.ltweaks.xposed.google;

import android.app.Notification;
import android.app.NotificationManager;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2018/1/10.
 */
@XposedLoad(packages = PackageNames.GMS, prefs = R.string.key_google_block_system_update_notification)
public class XposedGoogleBlockSystemUpdateNotification extends XposedBase {
    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(NotificationManager.class, "notify", String.class, int.class, Notification.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if ("com.google.android.gms.update.control.NotificationControl".equals(param.args[0])) {
                    Logger.i("Block system update notification.");
                    param.setResult(null);
                }
            }
        });
    }
}
