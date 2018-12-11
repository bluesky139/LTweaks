package li.lingfeng.ltweaks.xposed.system;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

@XposedLoad(packages = PackageNames.ANDROID, prefs = R.string.key_debug_mtp_always)
public class XposedMTPAlways extends XposedBase {
    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod("com.android.server.usb.UsbDeviceManager$UsbHandler", "setEnabledFunctions", String.class, boolean.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[0] == null) {
                    Logger.d("setEnabledFunctions null -> mtp");
                    param.args[0] = "mtp";
                }
            }
        });
    }
}
