package li.lingfeng.ltweaks.xposed.system;

import android.content.Context;
import android.provider.Settings;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.prefs.Prefs;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;

@XposedLoad(packages = PackageNames.ANDROID_SYSTEM_UI, prefs = R.string.key_quick_settings_tile_set_preconfigured_brightness)
public class XposedBrightnessTile extends XposedTile {
    @Override
    protected int getPriority() {
        return 2;
    }

    @Override
    protected String getTileName(boolean isOn) {
        int value = Prefs.instance().getIntFromString(R.string.key_quick_settings_tile_preconfigured_brightness, 0);
        if (value > 0) {
            return "Set " + value + " brightness";
        }
        return "Set xxx brightness";
    }

    @Override
    protected String getTileDesc() {
        return "Tap to set preconfigured brightness, long press to set auto brightness.";
    }

    @Override
    protected int getTileIcon(boolean isOn) {
        return ContextUtils.getDrawableId("ic_qs_brightness_auto_off");
    }

    @Override
    protected String getTileIconPackage() {
        return PackageNames.ANDROID_SYSTEM_UI;
    }

    @Override
    protected void onSwitch(Context context, boolean isOn) throws Throwable {
        int value = Prefs.instance().getIntFromString(R.string.key_quick_settings_tile_preconfigured_brightness, 0);
        if (value > 0) {
            Logger.i("Set brightness " + value);
            Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, value);
        }
    }

    @Override
    protected void onLongClick(Context context) throws Throwable {
        Logger.i("Set auto brightness.");
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
    }
}
