package li.lingfeng.ltweaks.xposed.google;

import android.app.Activity;
import android.net.Uri;
import android.view.MenuItem;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;

/**
 * Created by lilingfeng on 2017/7/28.
 */
@XposedLoad(packages = {
        PackageNames.CHROME,
        PackageNames.CHROME_BETA,
        PackageNames.CHROME_DEV,
        PackageNames.CHROME_CANARY
}, prefs = R.string.key_chrome_google_cache)
public class XposedChromeCache extends XposedChromeBase {

    private static final String MENU_CACHE = "View Google cached";

    @Override
    protected void handleLoadPackage() throws Throwable {
        super.handleLoadPackage();
        newMenu(MENU_CACHE, 1002, new NewMenuCallback() {
            @Override
            public void onOptionsItemSelected(Activity activity, MenuItem item, String url, boolean isCustomTab) {
                String cachedUrl = (url.startsWith("https") ? "https" : "http") + "://webcache.googleusercontent.com/search?q=cache:"
                        + Uri.encode(url);
                loadUrl(activity, cachedUrl);
            }
        });
    }
}
