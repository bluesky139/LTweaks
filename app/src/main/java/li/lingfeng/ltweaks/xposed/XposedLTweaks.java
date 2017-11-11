package li.lingfeng.ltweaks.xposed;

import android.os.Build;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;

/**
 * Created by lilingfeng on 2017/11/10.
 */
@XposedLoad(packages = PackageNames.L_TWEAKS, prefs = {})
public class XposedLTweaks extends XposedBase {

    private static final String CONTEXT_IMPL = "android.app.ContextImpl";

    @Override
    protected void handleLoadPackage() throws Throwable {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }
        findAndHookMethod(CONTEXT_IMPL, "checkMode", int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.v("LTweaks ignore ContextImpl checkMode.");
                param.setResult(null);
            }
        });
    }
}
