package li.lingfeng.ltweaks.xposed.communication;

import android.app.Activity;
import android.os.Bundle;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/6/27.
 */
@XposedLoad(packages = {
        PackageNames.QQ_LITE,
        PackageNames.QQ,
        PackageNames.QQ_INTERNATIONAL,
        PackageNames.TIM
}, prefs = R.string.key_qq_outer_browser)
public class XposedQQOuterBrowser extends XposedBase {

    private static final String BROWSER_ACTIVITY = "com.tencent.mobileqq.activity.QQBrowserActivity";

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookActivity(BROWSER_ACTIVITY, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                String url = activity.getIntent().getStringExtra("url");
                if (url != null && !url.isEmpty()) {
                    Logger.i("QQBrowserActivity url " + url);
                    ContextUtils.startBrowser(activity, url);
                    activity.finish();
                }
            }
        });
    }
}
