package li.lingfeng.ltweaks.xposed.entertainment;

import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/8/14.
 */
@XposedLoad(packages = {
        PackageNames.BILIBILI,
        PackageNames.BILIBILI_IN
}, prefs = R.string.key_bilibili_expand_following)
public class XposedBilibiliExpandFollowing extends XposedBase {

    private static final String UPPER_FEED_ITEM = "tv.danmaku.bili.tianma.api.UpperFeedList$UpperFeedItem";
    private static final String VIDEO_HOLDER = "tv.danmaku.bili.tianma.promo.up.UpFeedFragment$VideoHolder";

    @Override
    protected void handleLoadPackage() throws Throwable {
        Class clsUpperFeedItem = findClass(UPPER_FEED_ITEM);
        findAndHookMethodByParameterAndReturnTypes(VIDEO_HOLDER, void.class, clsUpperFeedItem, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                View recentWrapper = (View) XposedHelpers.getObjectField(param.thisObject, "recentWrapper");
                if (recentWrapper.getVisibility() == View.VISIBLE) {
                    Logger.i("recentWrapper click.");
                    recentWrapper.performClick();
                }
            }
        });
    }
}
