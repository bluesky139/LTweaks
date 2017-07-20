package li.lingfeng.ltweaks.xposed.google;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;
import android.util.Patterns;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.lang.ref.WeakReference;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.MyApplication;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.IntentActions;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/7/18.
 */
@XposedLoad(packages = PackageNames.CHROME, prefs = R.string.key_chrome_incognito_search)
public class XposedChromeIncognitoSearch extends XposedBase {

    private static final String INTENT_HANDLER = "org.chromium.chrome.browser.IntentHandler";
    private static final String TAB_CREATOR = "org.chromium.chrome.browser.tabmodel.ChromeTabCreator";
    private static final String LOAD_URL_PARAMS = "org.chromium.content_public.browser.LoadUrlParams";
    private static final String TAB_LAUNCH_TYPE = "org.chromium.chrome.browser.tabmodel.TabModel$TabLaunchType";
    private static final String TAB = "org.chromium.chrome.browser.tab.Tab";
    private static final String MENU_PROPERTIES_DELEGATE = "org.chromium.chrome.browser.appmenu.AppMenuPropertiesDelegate";
    private static final String CUSTOM_MENU_PROPERTIES_DELEGATE = "org.chromium.chrome.browser.customtabs.CustomTabAppMenuPropertiesDelegate";
    private static final String TABBED_ACTIVITY = "org.chromium.chrome.browser.ChromeTabbedActivity";
    private static final String CUSTOM_ACTIVITY = "org.chromium.chrome.browser.customtabs.CustomTabActivity";
    private static final String CONTEXT_MENU_HELPER = "org.chromium.chrome.browser.contextmenu.ContextMenuHelper";
    private static final String CONTEXT_MENU_POPULATOR = "org.chromium.chrome.browser.contextmenu.ChromeContextMenuPopulator";
    private static final String CONTEXT_MENU_PARAMS = "org.chromium.chrome.browser.contextmenu.ContextMenuParams";
    private static final String SELECTION_POPUP_CONTROLLER = "org.chromium.content.browser.SelectionPopupController";
    private static final String MENU_INCOGNITO = "Open in incognito";

    @Override
    protected void handleLoadPackage() throws Throwable {
        // Allow open url in incognito.
        findAndHookMethod(INTENT_HANDLER, "shouldIgnoreIntent", Intent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Intent intent = (Intent) param.args[0];
                if (intent != null && intent.getBooleanExtra("from_ltweaks", false)) {
                    Logger.i("shouldIgnoreIntent() return false");
                    param.setResult(false);
                }
            }
        });

        final Class clsLoadUrlParams = findClass(LOAD_URL_PARAMS);
        final Class clsTabLaunchType = findClass(TAB_LAUNCH_TYPE);
        final Class clsTab = findClass(TAB);
        findAndHookMethod(TAB_CREATOR, "createNewTab", clsLoadUrlParams, clsTabLaunchType, clsTab, Intent.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Intent intent = (Intent) param.args[3];
                if (intent != null && intent.getBooleanExtra("from_ltweaks_external", false)) {
                    Logger.i("Set mAppAssociatedWith to LTweaks.");
                    XposedHelpers.setObjectField(param.getResult(), "mAppAssociatedWith", PackageNames.L_TWEAKS);
                    Object enumFromExternalApp = XposedHelpers.getStaticObjectField(clsTabLaunchType, "FROM_EXTERNAL_APP");
                    XposedHelpers.setObjectField(param.getResult(), "mLaunchType", enumFromExternalApp);
                }
            }
        });


        // Menu "Open in incognito" in Chrome and CustomTab.
        findAndHookMethod(MENU_PROPERTIES_DELEGATE, "prepareMenu", Menu.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                prepareMenu(param);
            }
        });

        findAndHookMethod(CUSTOM_MENU_PROPERTIES_DELEGATE, "prepareMenu", Menu.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                prepareMenu(param);
            }
        });

        findAndHookActivity(TABBED_ACTIVITY, "onOptionsItemSelected", MenuItem.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                onOptionsItemSelected(param, false);
            }
        });

        findAndHookActivity(CUSTOM_ACTIVITY, "onOptionsItemSelected", MenuItem.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                onOptionsItemSelected(param, true);
            }
        });


        // Context menu "Open in incognito" in CustomTab.
        Class clsContextMenuParams = findClass(CONTEXT_MENU_PARAMS);
        findAndHookMethod(CONTEXT_MENU_POPULATOR, "buildContextMenu",
                ContextMenu.class, Context.class, clsContextMenuParams, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                buildContextMenu(param);
            }
        });

        findAndHookMethod(CONTEXT_MENU_HELPER, "onMenuItemClick", MenuItem.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                onMenuItemClick(param);
            }
        });


        // Stay in Chrome if "Incognito Search" with selected text from Chrome.
        findAndHookMethod(SELECTION_POPUP_CONTROLLER, "onActionItemClicked", ActionMode.class, MenuItem.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                MenuItem item = (MenuItem) param.args[1];
                if (ContextUtils.createLTweaksContext().getString(R.string.process_text_incognito_search)
                        .equals(item.getTitle())) {
                    boolean isFromLTweaksExternal = true;
                    Object windowAndroid = XposedHelpers.getObjectField(param.thisObject, "mWindowAndroid");
                    WeakReference weakReference = (WeakReference) XposedHelpers.callMethod(windowAndroid, "getActivity");
                    if (weakReference.get().getClass().getName().equals(TABBED_ACTIVITY)) {
                        isFromLTweaksExternal = false;
                    }
                    item.getIntent().putExtra("from_ltweaks_external", isFromLTweaksExternal);
                }
            }
        });
    }

    private void prepareMenu(XC_MethodHook.MethodHookParam param) {
        Menu menu = (Menu) param.args[0];
        Activity activity = (Activity) XposedHelpers.getObjectField(param.thisObject, "mActivity");
        Object activityTab = XposedHelpers.callMethod(activity, "getActivityTab");
        String url = (String) XposedHelpers.callMethod(activityTab, "getUrl");
        addMenu(menu, url);
    }

    private void buildContextMenu(XC_MethodHook.MethodHookParam param) {
        int mode = XposedHelpers.getIntField(param.thisObject, "mMode");
        if (mode != 1) {
            return;
        }
        ContextMenu menu = (ContextMenu) param.args[0];
        Object menuParams = param.args[2];
        String linkUrl = (String) XposedHelpers.getObjectField(menuParams, "mLinkUrl");
        addMenu(menu, linkUrl);
    }

    private void addMenu(Menu menu,  String url) {
        if (Patterns.WEB_URL.matcher(url).matches()) {
            if (getIncognitoMenu(menu) == null) {
                Logger.i("Add menu \"" + MENU_INCOGNITO + "\"");
                menu.add(MENU_INCOGNITO);
            }
            Logger.i("Set \"" + MENU_INCOGNITO + "\" visible.");
            getIncognitoMenu(menu).setVisible(true);
        } else {
            Logger.i("Set \"" + MENU_INCOGNITO + "\" invisible.");
            getIncognitoMenu(menu).setVisible(false);
        }
    }

    private void onOptionsItemSelected(XC_MethodHook.MethodHookParam param, boolean isFromLTweaksExternal) {
        MenuItem item = (MenuItem) param.args[0];
        if (MENU_INCOGNITO.equals(item.getTitle())) {
            Activity activity = (Activity) param.thisObject;
            Object activityTab = XposedHelpers.callMethod(activity, "getActivityTab");
            String url = (String) XposedHelpers.callMethod(activityTab, "getUrl");
            Logger.i("Open in incognito: " + url);

            Intent intent = new Intent(IntentActions.ACTION_CHROME_INCOGNITO);
            intent.setData(Uri.parse(url));
            intent.putExtra("from_ltweaks_external", isFromLTweaksExternal);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            activity.startActivity(intent);
            param.setResult(true);
        }
    }

    private void onMenuItemClick(XC_MethodHook.MethodHookParam param) {
        MenuItem item = (MenuItem) param.args[0];
        if (MENU_INCOGNITO.equals(item.getTitle())) {
            Object menuParams = XposedHelpers.getObjectField(param.thisObject, "mCurrentContextMenuParams");
            String linkUrl = (String) XposedHelpers.getObjectField(menuParams, "mLinkUrl");
            Logger.i("Open link in incognito: " + linkUrl);

            Intent intent = new Intent(IntentActions.ACTION_CHROME_INCOGNITO);
            intent.setData(Uri.parse(linkUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            MyApplication.instance().startActivity(intent);
            param.setResult(true);
        }
    }

    private MenuItem getIncognitoMenu(Menu menu) {
        for (int i = 0; i < menu.size(); ++i) {
            MenuItem item = menu.getItem(i);
            if (MENU_INCOGNITO.equals(item.getTitle())) {
                return item;
            }
        }
        return null;
    }
}
