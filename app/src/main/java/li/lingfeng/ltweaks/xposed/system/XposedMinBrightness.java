package li.lingfeng.ltweaks.xposed.system;

import android.os.PowerManager;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.prefs.Prefs;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2018/4/19.
 */
@XposedLoad(packages = PackageNames.ANDROID_SYSTEM_UI, prefs = {})
public class XposedMinBrightness extends XposedBase {
    @Override
    protected void handleLoadPackage() throws Throwable {
        final int minBrightness = Prefs.instance().getIntFromString(R.string.key_display_min_brightness, 0);
        if (minBrightness <= 0) {
            return;
        }
        findAndHookMethod(PowerManager.class, "getMinimumScreenBrightnessSetting", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(minBrightness);
                Logger.v("getMinimumScreenBrightnessSetting return " + minBrightness);
            }
        });
    }
}
