package li.lingfeng.ltweaks.xposed.google;

import android.app.Activity;
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
}, prefs = R.string.key_chrome_wayback, loadAtActivityCreate = ClassNames.ACTIVITY)
public class XposedChromeWayback extends XposedChromeBase {

    @Override
    protected void handleLoadPackage() throws Throwable {
        super.handleLoadPackage();
        newMenu(ContextUtils.getLString(R.string.chrome_wayback_machine), 1003, new NewMenuCallback() {
            @Override
            public void onOptionsItemSelected(Activity activity, MenuItem item, String url, boolean isCustomTab) {
                loadUrl(activity, "https://web.archive.org/web/*/" + url);
            }
        });
    }
}
