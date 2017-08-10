package li.lingfeng.ltweaks.xposed.entertainment;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/8/10.
 */
@XposedLoad(packages = PackageNames.DOUBAN_MOVIE, prefs = R.string.key_douban_movie_remove_ads)
public class XposedDoubanMovieAds extends XposedBase {

    private static final String AD_SHOW_MANAGER = "com.douban.ad.AdShowManager";
    private static final String AD_MANAGER = "com.douban.ad.DoubanAdManager";

    @Override
    protected void handleLoadPackage() throws Throwable {
        hookAllMethods(AD_SHOW_MANAGER, "showAd", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("Prevent fullscreen ads.");
                param.setResult(null);
                Object onShowAdListener = param.args[3];
                if (onShowAdListener != null) {
                    XposedHelpers.callMethod(onShowAdListener, "onFailed");
                }
            }
        });

        hookAllMethods(AD_MANAGER, "requestAds", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("Prevent ads download.");
                param.setResult(null);
            }
        });
    }
}
