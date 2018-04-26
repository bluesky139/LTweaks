package li.lingfeng.ltweaks.xposed.system;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2018/4/26.
 */
@XposedLoad(packages = {}, prefs = R.string.key_web_search_to_browser, excludedPackages = PackageNames.ANDROID)
public class XposedWebSearchToBrowser extends XposedBase {
    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(Activity.class, "startActivity", Intent.class, Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Intent intent = (Intent) param.args[0];
                if (Intent.ACTION_WEB_SEARCH.equals(intent.getAction())) {
                    String query = intent.getStringExtra("query");
                    String url = "https://www.google.com/search?q=" + Uri.encode(query);
                    Activity activity = (Activity) param.thisObject;
                    ContextUtils.startBrowser(activity, url);
                    param.setResult(null);
                }
            }
        });
    }
}
