package li.lingfeng.ltweaks.xposed.entertainment;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2018/8/25.
 */
@XposedLoad(packages = PackageNames.STEAM, prefs = R.string.key_steam_chinese)
public class XposedSteamChinese extends XposedBase {

    private static final String USER_ACCOUNT_INFO = "com.valvesoftware.android.steam.community.LoggedInUserAccountInfo";

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(USER_ACCOUNT_INFO, "setCookie2", String.class, String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String key = (String) param.args[0];
                if (key.equals("Steam_Language")) {
                    Logger.i("Steam_Language " + param.args[1] + " -> schinese.");
                    param.args[1] = "schinese";
                }
            }
        });
    }
}
