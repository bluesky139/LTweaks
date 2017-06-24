package li.lingfeng.ltweaks.xposed.entertainment;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.Toast;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.ShareUtils;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/6/24.
 */
@XposedLoad(packages = PackageNames.STEAM, prefs = R.string.key_steam_share_url)
public class XposedSteamShare extends XposedSteam {

    @Override
    protected String newMenuName() {
        return "Share";
    }

    @Override
    protected void gotUrl(Activity activity, String url) {
        ShareUtils.shareText(activity, url);
    }
}
