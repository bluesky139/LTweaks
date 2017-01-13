package li.lingfeng.ltweaks.xposed;

import java.lang.reflect.Field;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

/**
 * Created by smallville on 2017/1/7.
 */
@XposedLoad(packages = "com.douban.movie", prefs = R.string.key_douban_movie_skip_splash)
public class XposedDoubanMovie implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        findAndHookMethod("com.douban.movie.MovieApplication", lpparam.classLoader, "onCreate", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Field sSplashShowed = param.thisObject.getClass().getDeclaredField("sSplashShowed");
                sSplashShowed.setAccessible(true);
                sSplashShowed.set(null, true);
            }
        });
    }
}
