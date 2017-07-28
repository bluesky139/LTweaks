package li.lingfeng.ltweaks.xposed.google;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;

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
import li.lingfeng.ltweaks.utils.Utils;

/**
 * Created by lilingfeng on 2017/7/18.
 */
@XposedLoad(packages = {
        PackageNames.CHROME,
        PackageNames.CHROME_BETA,
        PackageNames.CHROME_DEV,
        PackageNames.CHROME_CANARY
}, prefs = R.string.key_chrome_incognito_search)
public class XposedChromeIncognitoSearch extends XposedChromeBase {

    private static final String INTENT_HANDLER = "org.chromium.chrome.browser.IntentHandler";
    private static final String TAB_CREATOR = "org.chromium.chrome.browser.tabmodel.ChromeTabCreator";
    private static final String TAB_LAUNCH_TYPE = "org.chromium.chrome.browser.tabmodel.TabModel$TabLaunchType";
    private static final String TAB = "org.chromium.chrome.browser.tab.Tab";
    private static final String CONTEXT_MENU_HELPER = "org.chromium.chrome.browser.contextmenu.ContextMenuHelper";
    private static final String CONTEXT_MENU_POPULATOR = "org.chromium.chrome.browser.contextmenu.ChromeContextMenuPopulator";
    private static final String TAB_CONTEXT_MENU_POPULATOR = "org.chromium.chrome.browser.tab.TabContextMenuPopulator";
    private static final String CONTEXT_MENU_PARAMS = "org.chromium.chrome.browser.contextmenu.ContextMenuParams";
    private static final String SELECTION_POPUP_CONTROLLER = "org.chromium.content.browser.SelectionPopupController";
    private static final String MENU_INCOGNITO = "Open in incognito";

    @Override
    protected void handleLoadPackage() throws Throwable {
        super.handleLoadPackage();

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
        newMenu(MENU_INCOGNITO, 1001, new NewMenuCallback() {
            @Override
            public void onOptionsItemSelected(Activity activity, MenuItem item, String url, boolean isCustomTab) {
                Intent intent = new Intent(IntentActions.ACTION_CHROME_INCOGNITO);
                intent.setData(Uri.parse(url));
                intent.putExtra("from_ltweaks_external", isCustomTab);
                intent.putExtra("chrome_package_for_ltweaks", lpparam.packageName);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                activity.startActivity(intent);
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

    private void buildContextMenu(XC_MethodHook.MethodHookParam param) {
        int mode = XposedHelpers.getIntField(param.thisObject, "mMode");
        if (mode != 1) {
            return;
        }
        ContextMenu menu = (ContextMenu) param.args[0];
        Object menuParams = param.args[2];
        final String linkUrl = (String) XposedHelpers.getObjectField(menuParams, "mLinkUrl");
        MenuItem item = addMenu(menu, linkUrl, MENU_INCOGNITO, 1001);
        if (item != null) {
            item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    XposedChromeIncognitoSearch.this.onMenuItemClick(linkUrl);
                    return true;
                }
            });
        }
    }

    private void onMenuItemClick(XC_MethodHook.MethodHookParam param) {
        MenuItem item = (MenuItem) param.args[0];
        if (MENU_INCOGNITO.equals(item.getTitle())) {
            Object menuParams = XposedHelpers.getObjectField(param.thisObject, "mCurrentContextMenuParams");
            String linkUrl = (String) XposedHelpers.getObjectField(menuParams, "mLinkUrl");
            onMenuItemClick(linkUrl);
            param.setResult(true);
        }
    }

    private void onMenuItemClick(String linkUrl) {
        Logger.i("Open link in incognito: " + linkUrl);
        Intent intent = new Intent(IntentActions.ACTION_CHROME_INCOGNITO);
        intent.setData(Uri.parse(linkUrl));
        intent.putExtra("chrome_package_for_ltweaks", lpparam.packageName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MyApplication.instance().startActivity(intent);
    }

    private MenuItem getIncognitoMenu(Menu menu) {
        return Utils.findMenuItemByTitle(menu, MENU_INCOGNITO);
    }
}
