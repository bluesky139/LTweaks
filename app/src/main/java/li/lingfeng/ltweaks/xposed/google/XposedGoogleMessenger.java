package li.lingfeng.ltweaks.xposed.google;

import android.content.ComponentName;
import android.content.IntentFilter;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/1/7.
 */
@XposedLoad(packages = PackageNames.GOOGLE_MESSENGER, prefs = R.string.key_google_messenger_disable_direct_share)
public class XposedGoogleMessenger extends XposedBase {
    @Override
    public void handleLoadPackage() throws Throwable {
        findAndHookMethod("com.google.android.apps.messaging.shared.datamodel.BugleChooserTargetService",
                "onGetChooserTargets", ComponentName.class, IntentFilter.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(null);
            }
        });
    }
}
