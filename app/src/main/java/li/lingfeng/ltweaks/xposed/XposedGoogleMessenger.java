package li.lingfeng.ltweaks.xposed;

import android.content.ComponentName;
import android.content.IntentFilter;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

/**
 * Created by smallville on 2017/1/7.
 */
@XposedLoad(packages = "com.google.android.apps.messaging", prefs = R.string.key_google_messenger_disable_direct_share)
public class XposedGoogleMessenger implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        findAndHookMethod("com.google.android.apps.messaging.shared.datamodel.BugleChooserTargetService", lpparam.classLoader,
                "onGetChooserTargets", ComponentName.class, IntentFilter.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(null);
            }
        });
    }
}
