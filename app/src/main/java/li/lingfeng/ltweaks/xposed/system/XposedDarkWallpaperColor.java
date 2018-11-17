package li.lingfeng.ltweaks.xposed.system;

import android.app.WallpaperColors;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.RequiresApi;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

@XposedLoad(packages = PackageNames.ANDROID, prefs = R.string.key_display_dark_wallpaper_color)
public class XposedDarkWallpaperColor extends XposedBase {
    @Override
    protected void handleLoadPackage() throws Throwable {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            return;
        }

        findAndHookConstructor(WallpaperColors.class, Color.class, Color.class, Color.class, int.class, new XC_MethodHook() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.d("WallpaperColors constructor.");
                Logger.paramArgs(param.args);
                param.args[0] = Color.valueOf(Color.BLACK);
                param.args[1] = Color.valueOf(Color.BLACK);
                param.args[2] = null;
                param.args[3] = 6;
                Logger.i("WallpaperColors is changed, but keep it black.");
            }
        });
    }
}
