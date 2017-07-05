package li.lingfeng.ltweaks.xposed.entertainment;

import android.app.Activity;
import android.os.Build;
import android.view.MenuItem;
import android.webkit.WebView;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.ViewUtils;

/**
 * Created by lilingfeng on 2017/7/4.
 */
@XposedLoad(packages = PackageNames.STEAM, prefs = R.string.key_steam_go_top)
public class XposedSteamGoTop extends XposedSteam {
    @Override
    protected String newMenuName() {
        return "Go Top";
    }

    @Override
    protected int newMenuPriority() {
        return 1;
    }

    @Override
    protected int newMenuShowAsAction() {
        return MenuItem.SHOW_AS_ACTION_NEVER;
    }

    @Override
    protected void menuItemSelected() throws Throwable {
        Logger.i("Steam go top.");
        ViewUtils.executeJs(getWebView(), "document.getElementsByClassName('page_content_ctn')[0].scrollIntoView();");
    }
}
