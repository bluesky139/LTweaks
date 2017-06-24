package li.lingfeng.ltweaks.xposed.entertainment;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.Toast;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.ShareUtils;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/6/24.
 */

public abstract class XposedSteam extends XposedBase {

    private static final String MAIN_ACTIVITY = "com.valvesoftware.android.steam.community.activity.MainActivity";
    private MenuItem mMenuShare;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookActivity(MAIN_ACTIVITY, "onPrepareOptionsMenu", Menu.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Menu menu = (Menu) param.args[0];
                mMenuShare = menu.add(newMenuName());
            }
        });

        findAndHookActivity(MAIN_ACTIVITY, "onOptionsItemSelected", MenuItem.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (mMenuShare != (MenuItem) param.args[0]) {
                    return;
                }

                Activity activity = (Activity) param.thisObject;
                int idWebView = ContextUtils.getResId("webView", "id");
                WebView webView = (WebView) activity.findViewById(idWebView);
                if (webView == null) {
                    Toast.makeText(activity, "Error.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String url = webView.getUrl();
                Logger.i("Got url " + url);
                gotUrl(activity, url);
            }
        });
    }

    protected abstract String newMenuName();
    protected abstract void gotUrl(Activity activity, String url);
}
