package li.lingfeng.ltweaks.xposed.entertainment;

import java.lang.reflect.Field;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/1/7.
 */
@XposedLoad(packages = PackageNames.DOUBAN_MOVIE, prefs = R.string.key_douban_movie_skip_splash)
public class XposedDoubanMovie extends XposedBase {
    @Override
    public void handleLoadPackage() throws Throwable {
        findAndHookMethod("com.douban.movie.MovieApplication", "onCreate", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Field sSplashShowed = param.thisObject.getClass().getDeclaredField("sSplashShowed");
                sSplashShowed.setAccessible(true);
                sSplashShowed.set(null, true);
            }
        });
    }
}
