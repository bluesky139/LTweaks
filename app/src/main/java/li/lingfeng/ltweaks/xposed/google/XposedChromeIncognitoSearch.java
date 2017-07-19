package li.lingfeng.ltweaks.xposed.google;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.IntentActions;
import li.lingfeng.ltweaks.prefs.PackageNames;
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
    private static final String TABBED_ACTIVITY = "org.chromium.chrome.browser.ChromeTabbedActivity";
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


        // Menu "Open in incognito" in Chrome.
        findAndHookMethod(MENU_PROPERTIES_DELEGATE, "prepareMenu", Menu.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Menu menu = (Menu) param.args[0];
                if (getIncognitoMenu(menu) == null) {
                    Logger.i("Add menu \"" + MENU_INCOGNITO + "\"");
                    menu.add(MENU_INCOGNITO);
                }

                Activity activity = (Activity) XposedHelpers.getObjectField(param.thisObject, "mActivity");
                Object activityTab = XposedHelpers.callMethod(activity, "getActivityTab");
                String url = (String) XposedHelpers.callMethod(activityTab, "getUrl");
                if (Patterns.WEB_URL.matcher(url).matches()) {
                    Logger.i("Set \"" + MENU_INCOGNITO + "\" visible.");
                    getIncognitoMenu(menu).setVisible(true);
                } else {
                    Logger.i("Set \"" + MENU_INCOGNITO + "\" invisible.");
                    getIncognitoMenu(menu).setVisible(false);
                }
            }
        });

        findAndHookActivity(TABBED_ACTIVITY, "onOptionsItemSelected", MenuItem.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                MenuItem item = (MenuItem) param.args[0];
                if (MENU_INCOGNITO.equals(item.getTitle())) {
                    Activity activity = (Activity) param.thisObject;
                    Object activityTab = XposedHelpers.callMethod(activity, "getActivityTab");
                    String url = (String) XposedHelpers.callMethod(activityTab, "getUrl");
                    Logger.i("Open in incognito: " + url);

                    Intent intent = new Intent(IntentActions.ACTION_CHROME_INCOGNITO);
                    intent.setData(Uri.parse(url));
                    intent.putExtra("from_ltweaks_external", false);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    activity.startActivity(intent);
                }
            }
        });
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
