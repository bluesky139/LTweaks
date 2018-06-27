package li.lingfeng.ltweaks.xposed.google;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.MyApplication;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
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
}, prefs = R.string.key_chrome_incognito_search, loadAtActivityCreate = ClassNames.ACTIVITY)
public class XposedChromeIncognitoSearch extends XposedChromeBase {

    private static final String TAB = "org.chromium.chrome.browser.tab.Tab";
    private static final String TAB_STATE = "org.chromium.chrome.browser.TabState";
    private static final String CONTEXT_MENU_HELPER = "org.chromium.chrome.browser.contextmenu.ContextMenuHelper";
    private static final String CONTEXT_MENU_PARAMS = "org.chromium.chrome.browser.contextmenu.ContextMenuParams";
    private static String MENU_INCOGNITO;

    private XC_MethodHook.Unhook mTabbedActivityTabCreatorsHook;

    @Override
    protected void handleLoadPackage() throws Throwable {
        super.handleLoadPackage();
        MENU_INCOGNITO = ContextUtils.getLString(R.string.chrome_open_in_incognito);

        // Allow open url in incognito.
        /*findAndHookMethod("aia", "l", Intent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Intent intent = (Intent) param.args[0];
                if (intent != null && intent.getBooleanExtra("from_ltweaks", false)) {
                    Logger.i("shouldIgnoreIntent() return false");
                    param.setResult(false);
                    Logger.stackTrace();
                }
            }
        });*/

        findAndHookMethod(Intent.class, "getParcelableExtra", String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String key = (String) param.args[0];
                Intent intent = (Intent) param.thisObject;
                if (key.equals("trusted_application_code_extra") && intent.getBooleanExtra("from_ltweaks", false)) {
                    Logger.d("Return fake trusted_application_code_extra.");
                    Intent intent2 = new Intent();
                    intent2.setComponent(new ComponentName(MyApplication.instance(), "FakeClass"));
                    PendingIntent pendingIntent = PendingIntent.getActivity(MyApplication.instance(), 0, intent2, 0);
                    param.setResult(pendingIntent);
                }
            }
        });

        // Set FROM_EXTERNAL_APP for incognito tab.
        final Class clsTab = findClass(TAB);
        final Class clsTabState = findClass(TAB_STATE);
        Constructor constructor = Utils.findConstructorHasParameterType(clsTab.getConstructors(), clsTabState);
        final Class clsTabLaunchType =  Utils.findClassFromList(constructor.getParameterTypes(), new Utils.FindClassCallback() {
            @Override
            public boolean onClassCheck(Class cls) {
                return cls.isEnum() && Enum.valueOf(cls, "FROM_EXTERNAL_APP") != null;
            }
        });
        final Class clsLoadUrlParams = findClass(LOAD_URL_PARAMS);
        final Method method = XposedHelpers.findMethodsByExactParameters(findClass(TABBED_ACTIVITY), Pair.class)[0];

        mTabbedActivityTabCreatorsHook = XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (mTabbedActivityTabCreatorsHook == null) {
                    return;
                }
                mTabbedActivityTabCreatorsHook.unhook();
                mTabbedActivityTabCreatorsHook = null;

                Logger.d("Tabbed activity is creating tab creator.");
                Pair pair = (Pair) param.getResult();
                Class cls = pair.first.getClass();
                Method methodCreateTab = null;
                do {
                    Method[] methods = XposedHelpers.findMethodsByExactParameters(cls, clsTab, clsLoadUrlParams, clsTabLaunchType, clsTab, int.class, Intent.class);
                    if (methods.length > 0) {
                        methodCreateTab = methods[0];
                        Logger.d("methodCreateTab " + methodCreateTab);
                        break;
                    }
                    cls = cls.getSuperclass();
                } while (cls != Object.class);

                if (methodCreateTab != null) {
                    XposedBridge.hookMethod(methodCreateTab, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Intent intent = (Intent) param.args[4];
                            if (intent != null && intent.getBooleanExtra("from_ltweaks_external", false)) {
                                Logger.i("Set mAppAssociatedWith to LTweaks.");
                                Object tab = param.getResult();
                                //XposedHelpers.setObjectField(tab, "u", PackageNames.L_TWEAKS);
                                Enum enumFromExternalApp = Enum.valueOf(clsTabLaunchType, "FROM_EXTERNAL_APP");
                                Field field = XposedHelpers.findFirstFieldByExactType(tab.getClass(), clsTabLaunchType);
                                field.set(tab, enumFromExternalApp);
                            }
                        }
                    });
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
        final Class clsContextMenuParams = findClass(CONTEXT_MENU_PARAMS);
        findAndHookMethod(CONTEXT_MENU_HELPER, "onCreateContextMenu",
                ContextMenu.class, View.class, ContextMenu.ContextMenuInfo.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object activity = XposedHelpers.findFirstFieldByExactType(param.thisObject.getClass(), Activity.class).get(param.thisObject);
                if (!activity.getClass().getName().equals(CUSTOM_ACTIVITY)) {
                    return;
                }

                ContextMenu menu = (ContextMenu) param.args[0];
                Object menuParams = XposedHelpers.findFirstFieldByExactType(param.thisObject.getClass(), clsContextMenuParams).get(param.thisObject);
                Method method = Utils.findMethodFromList(clsContextMenuParams.getDeclaredMethods(), new Utils.FindMethodCallback() {
                    @Override
                    public boolean onMethodCheck(Method m) {
                        return Modifier.isPublic(m.getModifiers()) && m.getReturnType() == String.class
                                && m.getParameterTypes().length == 0;
                    }
                });
                final String url = (String) method.invoke(menuParams);
                MenuItem item = addMenu(menu, MENU_INCOGNITO, 1001, true);
                if (item != null) {
                    item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            XposedChromeIncognitoSearch.this.onMenuItemClick(url);
                            return true;
                        }
                    });
                }
            }
        });
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
}
