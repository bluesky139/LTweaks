package li.lingfeng.ltweaks.xposed.google;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/7/10.
 */
@XposedLoad(packages = PackageNames.GOOGLE_PLUS, prefs = R.string.key_google_plus_no_redirect)
public class XposedGooglePlusNoRedirect extends XposedBase {

    private static final Pattern sUrlReg = Pattern.compile("^https?://plus\\.url\\.google\\.com/url\\?q=(.+)$");

    @Override
    protected void handleLoadPackage() throws Throwable {
        hookAllMethods(Activity.class, "startActivity", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Intent intent = (Intent) param.args[0];
                String originalUrl = intent.getDataString();
                if (originalUrl == null) {
                    return;
                }

                Matcher matcher = sUrlReg.matcher(originalUrl);
                if (matcher.find()) {
                    String url = intent.getData().getQueryParameter("q");
                    if (url != null) {
                        Logger.i(originalUrl + " -> " + url);
                        intent.setData(Uri.parse(url));
                    }
                }
            }
        });
    }
}
