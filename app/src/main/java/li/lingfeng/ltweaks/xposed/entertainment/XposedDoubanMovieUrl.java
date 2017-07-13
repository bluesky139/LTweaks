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
import li.lingfeng.ltweaks.xposed.XposedCommon;

/**
 * Created by smallville on 2017/5/31.
 */
@XposedLoad(packages = PackageNames.ANDROID, prefs = R.string.key_douban_movie_url)
public class XposedDoubanMovieUrl extends XposedCommon {
    @Override
    protected void handleLoadPackage() throws Throwable {
        hookAndSetComponentExported(PackageNames.DOUBAN_MOVIE, ClassNames.DOUBAN_MOVIE_INTENT_HANDLER_ACTIVITY);
    }
}
