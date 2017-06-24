package li.lingfeng.ltweaks.xposed.entertainment;

import android.app.Activity;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/6/24.
 */
@XposedLoad(packages = PackageNames.STEAM, prefs = R.string.key_steam_database)
public class XposedSteamDatabase extends XposedSteam {

    @Override
    protected String newMenuName() {
        return "Steam Database";
    }

    @Override
    protected void gotUrl(Activity activity, String url) {
        Pattern pattern = Pattern.compile("^https?://store\\.steampowered\\.com/app/(\\d+)/");
        Matcher matcher = pattern.matcher(url);
        if (!matcher.find()) {
            Toast.makeText(activity, "Can't find game id.", Toast.LENGTH_SHORT).show();
            return;
        }

        String gameId = matcher.group(1);
        ContextUtils.startBrowser(activity, "https://steamdb.info/app/" + gameId + "/");
    }
}
