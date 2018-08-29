package li.lingfeng.ltweaks.xposed.communication;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2018/8/29.
 */
@XposedLoad(packages = {
        PackageNames.QQ_LITE,
        PackageNames.QQ,
        PackageNames.QQ_INTERNATIONAL,
        PackageNames.TIM
}, prefs = R.string.key_qq_hide_weak_net_toast)
public class XposedQQHideWeakNetToast extends XposedBase {
    @Override
    protected void handleLoadPackage() throws Throwable {
        hookAllMethods("com.tencent.mobileqq.servlet.PushServlet", "onReceive", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String serviceCmd = (String) XposedHelpers.callMethod(param.args[1], "getServiceCmd");
                if ("cmd_connWeakNet".equals(serviceCmd)) {
                    Logger.i("Hide weak net toast.");
                    param.setResult(null);
                }
            }
        });
    }
}
