package li.lingfeng.ltweaks.xposed.google;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/7/18.
 */
@XposedLoad(packages = PackageNames.GOOGLE, prefs = R.string.key_google_com_domain)
public class XposedGoogleComDomain extends XposedBase {

    private static final String SEARCH_DOMAIN_PROPERTIES = "com.google.android.apps.gsa.search.core.google.SearchDomainProperties";

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(SEARCH_DOMAIN_PROPERTIES, "getSearchDomain", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("getSearchDomain() return www.google.com");
                param.setResult("www.google.com");
            }
        });
    }
}
