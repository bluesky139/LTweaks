package li.lingfeng.ltweaks.xposed.communication;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2018/5/14.
 */
@XposedLoad(packages = PackageNames.TT_RSS, prefs = R.string.key_ttrss_disable_show_hide_on_scroll)
public class XposedDisableShowHideOnScroll extends XposedBase {

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(ClassNames.TOOLBAR_ACTIONBAR, "hide", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(null);
            }
        });
    }
}
