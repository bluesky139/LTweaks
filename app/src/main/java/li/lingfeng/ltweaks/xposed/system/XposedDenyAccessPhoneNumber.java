package li.lingfeng.ltweaks.xposed.system;

import android.os.Build;
import android.os.Process;
import android.telephony.TelephonyManager;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2018/1/25.
 */
@XposedLoad(packages = {}, prefs = R.string.key_phone_deny_access_phone_number, excludedPackages = { PackageNames.ANDROID })
public class XposedDenyAccessPhoneNumber extends XposedBase {
    @Override
    protected void handleLoadPackage() throws Throwable {
        if (lpparam.appInfo.uid < Process.FIRST_APPLICATION_UID || lpparam.appInfo.uid > Process.LAST_APPLICATION_UID) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            findAndHookMethod(TelephonyManager.class, "getLine1Number", int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(null);
                }
            });
        } else {
            findAndHookMethod(TelephonyManager.class, "getLine1NumberForSubscriber", int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(null);
                }
            });
        }
    }
}
