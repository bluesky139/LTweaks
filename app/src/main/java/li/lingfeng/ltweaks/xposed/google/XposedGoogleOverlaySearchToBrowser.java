package li.lingfeng.ltweaks.xposed.google;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2018/4/19.
 */
@XposedLoad(packages = PackageNames.GOOGLE, prefs = R.string.key_google_overlay_search_to_browser)
public class XposedGoogleOverlaySearchToBrowser extends XposedBase {

    private static final String QUERY_ENTRY_ACTIVITY = "com.google.android.apps.gsa.queryentry.QueryEntryActivity";
    private static final String DYNAMIC_HOST_ACTIVITY = "com.google.android.apps.gsa.velour.dynamichosts.VelvetThemedDynamicHostActivity";

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookActivity(QUERY_ENTRY_ACTIVITY, "startActivity", Intent.class, Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Intent intent = (Intent) param.args[0];
                if (intent.getComponent() != null && intent.getComponent().getClassName().equals(DYNAMIC_HOST_ACTIVITY)) {
                    Object query = intent.getExtras().get("velvet-query");
                    if (query != null) {
                        Pattern pattern = Pattern.compile("text from user: \"([^/]+)\"/");
                        Matcher matcher = pattern.matcher(query.toString());
                        if (matcher.find()) {
                            String url = "https://www.google.com/search?q=" + Uri.encode(matcher.group(1));
                            Activity activity = (Activity) param.thisObject;
                            ContextUtils.startBrowser(activity, url);
                            activity.finish();
                            param.setResult(null);
                        } else {
                            Logger.e("Can't find text from user.");
                        }
                    } else {
                        Logger.e("Can't find velvet-query in intent.");
                    }
                }
            }
        });
    }
}
