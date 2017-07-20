package li.lingfeng.ltweaks.xposed.entertainment;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.Toast;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/6/24.
 */

public abstract class XposedSteam extends XposedBase {

    protected static final String MAIN_ACTIVITY = "com.valvesoftware.android.steam.community.activity.MainActivity";
    protected Activity mActivity;
    private MenuItem mMenuShare;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(ClassNames.FRAGMENT_MANAGER_IMPL, "dispatchPrepareOptionsMenu", Menu.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Menu menu = (Menu) param.args[0];
                mMenuShare = menu.add(Menu.NONE, Menu.NONE, newMenuPriority(), newMenuName());
                mMenuShare.setShowAsAction(newMenuShowAsAction());
            }
        });

        findAndHookActivity(MAIN_ACTIVITY, "onOptionsItemSelected", MenuItem.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (mMenuShare != (MenuItem) param.args[0]) {
                    return;
                }
                mActivity = (Activity) param.thisObject;
                try {
                    menuItemSelected();
                } catch (Throwable e) {
                    Logger.stackTrace(e);
                }
            }
        });

        findAndHookActivity(MAIN_ACTIVITY, "onDestroy", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mActivity = null;
            }
        });
    }

    protected WebView getWebView() {
        int idWebView = ContextUtils.getResId("webView", "id");
        return (WebView) mActivity.findViewById(idWebView);
    }

    protected String getUrl() {
        WebView webView = getWebView();
        if (webView == null) {
            Toast.makeText(mActivity, "Error.", Toast.LENGTH_SHORT).show();
            return null;
        }

        String url = webView.getUrl();
        Logger.i("Got url " + url);
        return url;
    }

    protected abstract String newMenuName();
    protected abstract int newMenuPriority();
    protected abstract int newMenuShowAsAction();
    protected abstract void menuItemSelected() throws Throwable;
}
