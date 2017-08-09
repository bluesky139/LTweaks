package li.lingfeng.ltweaks.xposed.communication;

import android.app.Activity;
import android.content.Intent;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedCommon;

/**
 * Created by lilingfeng on 2017/8/8.
 */
@XposedLoad(packages = {
        PackageNames.QQ_LITE,
        PackageNames.QQ,
        PackageNames.QQ_INTERNATIONAL,
        PackageNames.TIM,
        PackageNames.ANDROID
}, prefs = R.string.key_qq_exit_directly_by_sharing)
public class XposedQQDirectlyExitBySharing extends XposedCommon {

    private static final String FORWARD_RECENT_ACTIVITY = "com.tencent.mobileqq.activity.ForwardRecentActivity";

    @Override
    protected void handleLoadPackage() throws Throwable {
        if (lpparam.packageName.equals(PackageNames.ANDROID)) {
            for (String packageName : new String[] {
                    PackageNames.QQ_LITE,
                    PackageNames.QQ,
                    PackageNames.QQ_INTERNATIONAL,
                    PackageNames.TIM
            }) {
                hookAndSetComponentExported(packageName, FORWARD_RECENT_ACTIVITY);
                hookAndSetComponentExported(packageName, ClassNames.QQ_CHAT_ACTIVITY);
            }
        } else {
            hookQQ();
        }
    }

    private void hookQQ() {
        hookAllMethods(Activity.class, "startActivityForResult", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!param.thisObject.getClass().getName().equals(FORWARD_RECENT_ACTIVITY)) {
                    return;
                }
                Logger.i("QQ set isBack2Root to false.");
                Intent intent = (Intent) param.args[0];
                intent.putExtra("isBack2Root", false);
                intent.setFlags(0);
            }
        });
    }
}
