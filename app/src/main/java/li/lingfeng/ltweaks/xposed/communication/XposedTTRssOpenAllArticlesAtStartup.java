package li.lingfeng.ltweaks.xposed.communication;

import android.os.Bundle;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2018/7/11.
 */
@XposedLoad(packages = PackageNames.TT_RSS, prefs = R.string.key_ttrss_open_all_articles_at_startup)
public class XposedTTRssOpenAllArticlesAtStartup extends XposedBase {

    private static final String MASTER_ACTIVITY = "org.fox.ttrss.MasterActivity";
    private static final String FEED = "org.fox.ttrss.types.Feed";
    private boolean mIsMasterActivityCreating = false;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(MASTER_ACTIVITY, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mIsMasterActivityCreating = true;
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mIsMasterActivityCreating = false;
            }
        });

        findAndHookConstructor(FEED, int.class, String.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mIsMasterActivityCreating) {
                    Logger.i("Open all articles at startup.");
                    param.args[0] = -4;
                    param.args[1] = ContextUtils.getString("feed_all_articles");
                }
            }
        });

        /*findAndHookMethod("org.fox.ttrss.ApiRequest", "execute", HashMap.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                HashMap<String, String> map = (HashMap) param.args[0];
                Logger.map(map);
            }
        });*/
    }
}
