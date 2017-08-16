package li.lingfeng.ltweaks.xposed.google;

import android.app.Activity;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.Utils;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/7/28.
 */

public class XposedChromeBase extends XposedBase {

    protected static final String MENU_PROPERTIES_DELEGATE = "org.chromium.chrome.browser.appmenu.AppMenuPropertiesDelegate";
    protected static final String CUSTOM_MENU_PROPERTIES_DELEGATE = "org.chromium.chrome.browser.customtabs.CustomTabAppMenuPropertiesDelegate";
    protected static final String TABBED_ACTIVITY = "org.chromium.chrome.browser.ChromeTabbedActivity";
    protected static final String CUSTOM_ACTIVITY = "org.chromium.chrome.browser.customtabs.CustomTabActivity";
    protected static final String LOAD_URL_PARAMS = "org.chromium.content_public.browser.LoadUrlParams";

    @Override
    protected void handleLoadPackage() throws Throwable {
    }

    protected interface NewMenuCallback {
        void onOptionsItemSelected(Activity activity, MenuItem item, String url, boolean isCustomTab);
    }

    // New menu in Chrome and CustomTab.
    protected void newMenu(final String title, final int order, final NewMenuCallback selectedCallback) {
        findAndHookMethod(MENU_PROPERTIES_DELEGATE, "prepareMenu", Menu.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                prepareMenu(param, title, order);
            }
        });

        findAndHookMethod(CUSTOM_MENU_PROPERTIES_DELEGATE, "prepareMenu", Menu.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                prepareMenu(param, title, order);
            }
        });

        findAndHookActivity(TABBED_ACTIVITY, "onOptionsItemSelected", MenuItem.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                onOptionsItemSelected(param, title, false, selectedCallback);
            }
        });

        findAndHookActivity(CUSTOM_ACTIVITY, "onOptionsItemSelected", MenuItem.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                onOptionsItemSelected(param, title, true, selectedCallback);
            }
        });
    }

    private void prepareMenu(XC_MethodHook.MethodHookParam param, String title, int order) {
        Menu menu = (Menu) param.args[0];
        Activity activity = (Activity) XposedHelpers.getObjectField(param.thisObject, "mActivity");
        Object activityTab = XposedHelpers.callMethod(activity, "getActivityTab");
        if (activityTab == null) {
            return;
        }
        String url = (String) XposedHelpers.callMethod(activityTab, "getUrl");
        addMenu(menu, url, title, order);
    }

    protected MenuItem addMenu(Menu menu, String url, String title, int order) {
        MenuItem item = Utils.findMenuItemByTitle(menu, title);
        if (Utils.isUrl(url)) {
            if (item == null) {
                Logger.i("Add menu \"" + title + "\"");
                item = menu.add(Menu.NONE, Menu.NONE, order, title);
            }
            Logger.i("Set \"" + title + "\" visible.");
            item.setVisible(true);
        } else {
            if (item != null) {
                Logger.i("Set \"" + title + "\" invisible.");
                item.setVisible(false);
            }
        }
        return item;
    }

    private void onOptionsItemSelected(XC_MethodHook.MethodHookParam param, String title, boolean isCustomTab,
                                       NewMenuCallback selectedCallback) {
        MenuItem item = (MenuItem) param.args[0];
        if (title.equals(item.getTitle())) {
            Activity activity = (Activity) param.thisObject;
            Object activityTab = XposedHelpers.callMethod(activity, "getActivityTab");
            String url = (String) XposedHelpers.callMethod(activityTab, "getUrl");
            Logger.i("Menu \"" + title + "\" is clicked, url " + url);
            selectedCallback.onOptionsItemSelected(activity, item, url, isCustomTab);
            param.setResult(true);
        }
    }

    protected void loadUrl(Activity activity, String url) {
        Logger.i("loadUrl " + url);
        Object tab = XposedHelpers.callMethod(activity, "getActivityTab");
        Class clsLoadUrlParams = findClass(LOAD_URL_PARAMS);
        Object loadUrlParams = XposedHelpers.newInstance(clsLoadUrlParams, url);
        XposedHelpers.callMethod(tab, "loadUrl", loadUrlParams);
    }
}
