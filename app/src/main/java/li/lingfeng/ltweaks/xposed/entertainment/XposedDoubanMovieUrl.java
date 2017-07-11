package li.lingfeng.ltweaks.xposed.entertainment;

import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageParser;

import java.lang.reflect.Field;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/5/31.
 */
@XposedLoad(packages = PackageNames.ANDROID, prefs = R.string.key_douban_movie_url)
public class XposedDoubanMovieUrl extends XposedBase {
    @Override
    protected void handleLoadPackage() throws Throwable {
        hookAllMethods(ClassNames.PACKAGE_PARSER, "parseActivity", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                PackageParser.Package owner = (PackageParser.Package) param.args[0];
                if (!owner.packageName.equals(PackageNames.DOUBAN_MOVIE)) {
                    return;
                }

                PackageParser.Activity activity = (PackageParser.Activity) param.getResult();
                Field fieldInfo = PackageParser.Activity.class.getDeclaredField("info");
                fieldInfo.setAccessible(true);
                ActivityInfo info = (ActivityInfo) fieldInfo.get(activity);
                if (!info.name.equals(ClassNames.DOUBAN_MOVIE_INTENT_HANDLER_ACTIVITY)) {
                    return;
                }

                Logger.i("Set Douban Movie activity exported to true.");
                info.exported = true;
                info.launchMode = ActivityInfo.LAUNCH_MULTIPLE;
            }
        });
    }
}
