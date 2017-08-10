package li.lingfeng.ltweaks.xposed.google;

import android.app.Activity;
import android.view.MenuItem;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;

/**
 * Created by lilingfeng on 2017/8/10.
 */
@XposedLoad(packages = {
        PackageNames.CHROME,
        PackageNames.CHROME_BETA,
        PackageNames.CHROME_DEV,
        PackageNames.CHROME_CANARY
}, prefs = R.string.key_chrome_go_top_or_bottom)
public class XposedChromeGoTopOrBottom extends XposedChromeBase {

    private static final String MENU_GO_TOP = "Go Top";
    private static final String MENU_GO_BOTTOM = "Go Bottom";

    @Override
    protected void handleLoadPackage() throws Throwable {
        super.handleLoadPackage();
        newMenu(MENU_GO_TOP, 1004, new NewMenuCallback() {
            @Override
            public void onOptionsItemSelected(Activity activity, MenuItem item, String url, boolean isCustomTab) {
                loadUrl(activity, "javascript:window.scrollTo(0, 0);");
            }
        });
        newMenu(MENU_GO_BOTTOM, 1005, new NewMenuCallback() {
            @Override
            public void onOptionsItemSelected(Activity activity, MenuItem item, String url, boolean isCustomTab) {
                loadUrl(activity, "javascript:window.scrollTo(0, document.body.scrollHeight);");
            }
        });
    }
}
