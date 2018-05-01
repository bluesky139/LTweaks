package li.lingfeng.ltweaks.xposed.system;

import android.content.res.XResources;

import de.robv.android.xposed.IXposedHookZygoteInit;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.ZygoteLoad;
import li.lingfeng.ltweaks.prefs.Prefs;
import li.lingfeng.ltweaks.utils.Logger;

/**
 * Created by smallville on 2018/4/19.
 */
@ZygoteLoad(prefs = {})
public class XposedMinBrightness implements IXposedHookZygoteInit {

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        int minBrightness = Prefs.instance().getIntFromString(R.string.key_display_min_brightness, 0);
        if (minBrightness <= 0) {
            return;
        }
        Logger.i("Set min brightness " + minBrightness);
        XResources.setSystemWideReplacement("android", "integer", "config_screenBrightnessSettingMinimum", minBrightness);
    }
}
