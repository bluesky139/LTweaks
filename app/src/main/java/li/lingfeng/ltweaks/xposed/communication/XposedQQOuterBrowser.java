package li.lingfeng.ltweaks.xposed.communication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;

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

    private static final String BROWSER_DELEGATED_ACTIVITY = "com.tencent.mobileqq.activity.QQBrowserDelegationActivity";

    @Override
    protected void handleLoadPackage() throws Throwable {
        hookAllMethods(Activity.class, "startActivityForResult", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Intent intent = (Intent) param.args[0];
                if (intent.getComponent() == null
                        || !intent.getComponent().getClassName().equals(BROWSER_DELEGATED_ACTIVITY)) {
                    return;
                }

                Activity activity = (Activity) param.thisObject;
                String url = intent.getStringExtra("url");
                if (Patterns.WEB_URL.matcher(url).matches()) {
                    Logger.i("QQ url " + url);
                    ContextUtils.startBrowser(activity, url);
                    param.setResult(null);
                }
            }
        });
    }
}
