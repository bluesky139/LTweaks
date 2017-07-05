package li.lingfeng.ltweaks.xposed.entertainment;

import android.view.MenuItem;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.ViewUtils;

/**
 * Created by lilingfeng on 2017/7/4.
 */
@XposedLoad(packages = PackageNames.STEAM, prefs = R.string.key_steam_go_reviews)
public class XposedSteamGoReviews extends XposedSteam {
    @Override
    protected String newMenuName() {
        return "Go Reviews";
    }

    @Override
    protected int newMenuPriority() {
        return 2;
    }

    @Override
    protected int newMenuShowAsAction() {
        return MenuItem.SHOW_AS_ACTION_NEVER;
    }

    @Override
    protected void menuItemSelected() throws Throwable {
        Logger.i("Steam go reviews.");
        ViewUtils.executeJs(getWebView(), "document.getElementById('Reviews_summary').scrollIntoView();");
    }
}
