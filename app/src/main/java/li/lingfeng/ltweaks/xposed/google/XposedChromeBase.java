package li.lingfeng.ltweaks.xposed.google;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.PopupMenu;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.Utils;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/7/28.
 */

public class XposedChromeBase extends XposedBase {

    protected static final String TABBED_ACTIVITY = "org.chromium.chrome.browser.ChromeTabbedActivity";
    protected static final String CUSTOM_ACTIVITY = "org.chromium.chrome.browser.customtabs.CustomTabActivity";
    protected static final String LOAD_URL_PARAMS = "org.chromium.content_public.browser.LoadUrlParams";
    protected static final String TAB = "org.chromium.chrome.browser.tab.Tab";

    @Override
    protected void handleLoadPackage() throws Throwable {
    }

    protected interface NewMenuCallback {
        void onOptionsItemSelected(Activity activity, MenuItem item, String url, boolean isCustomTab);
    }

    // New menu in Chrome and CustomTab.
    protected void newMenu(final String title, final int order, final NewMenuCallback selectedCallback) {
        final int idPageMenu = ContextUtils.getIdId("PAGE_MENU");
        findAndHookMethod(ClassNames.MENU_BUILDER, "setGroupVisible", int.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                int menuId = (int) param.args[0];
                if (menuId == idPageMenu) {
                    boolean visible = (boolean) param.args[1];
                    Logger.i("PAGE_MENU visible " + visible);
                    Menu menu = (Menu) param.thisObject;
                    addMenu(menu, title, order, visible);
                }
            }
        });

        final int idCustomTabMenu = ContextUtils.getMenuId("custom_tabs_menu");
        findAndHookMethod(PopupMenu.class, "inflate", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                int id = (int) param.args[0];
                if (id == idCustomTabMenu) {
                    Logger.i("Inflate custom_tabs_menu.");
                    PopupMenu popupMenu = (PopupMenu) param.thisObject;
                    Menu menu = popupMenu.getMenu();
                    addMenu(menu, title, order, true);
                }
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

    protected MenuItem addMenu(Menu menu, String title, int order, boolean visible) {
        MenuItem item = Utils.findMenuItemByTitle(menu, title);
        if (visible) {
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
            try {
                Object activityTab = getCurrentTab(activity);
                String url = (String) XposedHelpers.callMethod(activityTab, "getUrl");
                Logger.i("Menu \"" + title + "\" is clicked, url " + url);
                selectedCallback.onOptionsItemSelected(activity, item, url, isCustomTab);
            } catch (Throwable e) {
                Toast.makeText(activity, "Error.", Toast.LENGTH_SHORT).show();
                Logger.stackTrace(e);
            }
            param.setResult(true);
        }
    }

    protected void loadUrl(Activity activity, String url) {
        Logger.i("loadUrl " + url);
        try {
            Object tab = getCurrentTab(activity);
            final Class clsLoadUrlParams = findClass(LOAD_URL_PARAMS);
            Method method = Utils.findMethodFromList(tab.getClass().getDeclaredMethods(), new Utils.FindMethodCallback() {
                @Override
                public boolean onMethodCheck(Method m) {
                    return Modifier.isPublic(m.getModifiers()) && m.getParameterTypes().length == 1
                            && m.getParameterTypes()[0] == clsLoadUrlParams && m.getReturnType() == int.class;
                }
            });
            Object loadUrlParams = XposedHelpers.newInstance(clsLoadUrlParams, url);
            int ret = (int) method.invoke(tab, loadUrlParams);
            Logger.d("loadUrl return " + ret);
        } catch (Throwable e) {
            Toast.makeText(activity, "Error.", Toast.LENGTH_SHORT).show();
            Logger.stackTrace(e);
        }
    }

    private Object getCurrentTab(Activity activity) throws Throwable {
        Method[] methods = activity.getClass().getMethods();
        Method method = Utils.findMethodFromList(methods, new Utils.FindMethodCallback() {
            @Override
            public boolean onMethodCheck(Method m) {
                return m.getReturnType().getName().equals(TAB) && m.getParameterTypes().length == 0;
            }
        });
        if (method == null) {
            throw new RuntimeException("Can't find getCurrentTab method.");
        }
        return method.invoke(activity);
    }
}
