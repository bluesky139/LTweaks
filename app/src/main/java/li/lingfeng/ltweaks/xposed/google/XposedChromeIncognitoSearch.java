package li.lingfeng.ltweaks.xposed.google;

import android.content.Intent;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
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

    @Override
    protected void handleLoadPackage() throws Throwable {
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
                if (intent != null && intent.getBooleanExtra("from_ltweaks", false)) {
                    Logger.i("Set mAppAssociatedWith to LTweaks.");
                    XposedHelpers.setObjectField(param.getResult(), "mAppAssociatedWith", PackageNames.L_TWEAKS);
                    Object enumFromExternalApp = XposedHelpers.getStaticObjectField(clsTabLaunchType, "FROM_EXTERNAL_APP");
                    XposedHelpers.setObjectField(param.getResult(), "mLaunchType", enumFromExternalApp);
                }
            }
        });
    }
}
