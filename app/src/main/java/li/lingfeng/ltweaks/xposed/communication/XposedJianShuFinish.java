package li.lingfeng.ltweaks.xposed.communication;

import android.app.Activity;
import android.app.Application;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/7/12.
 */
@XposedLoad(packages = PackageNames.JIAN_SHU, prefs = R.string.key_jianshu_finish)
public class XposedJianShuFinish extends XposedBase {
    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(Application.class, "registerActivityLifecycleCallbacks", Application.ActivityLifecycleCallbacks.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[0].getClass().getName().startsWith("com.baiji.jianshu")) {
                    Logger.i("Remove registerActivityLifecycleCallbacks");
                    param.setResult(null);
                }
            }
        });
    }
}
