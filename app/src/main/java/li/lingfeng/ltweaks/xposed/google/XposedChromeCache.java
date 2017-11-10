package li.lingfeng.ltweaks.xposed.google;

import android.app.Activity;
import android.net.Uri;
import android.view.MenuItem;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;

/**
 * Created by lilingfeng on 2017/7/28.
 */
@XposedLoad(packages = {
        PackageNames.CHROME,
        PackageNames.CHROME_BETA,
        PackageNames.CHROME_DEV,
        PackageNames.CHROME_CANARY
}, prefs = R.string.key_chrome_google_cache, loadAtActivityCreate = ClassNames.ACTIVITY)
public class XposedChromeCache extends XposedChromeBase {


    @Override
    protected void handleLoadPackage() throws Throwable {
        super.handleLoadPackage();
        newMenu(ContextUtils.getLString(R.string.chrome_google_cache), 1002, new NewMenuCallback() {
            @Override
            public void onOptionsItemSelected(Activity activity, MenuItem item, String url, boolean isCustomTab) {
                String cachedUrl = (url.startsWith("https") ? "https" : "http") + "://webcache.googleusercontent.com/search?q=cache:"
                        + Uri.encode(url);
                loadUrl(activity, cachedUrl);
            }
        });
    }
}
