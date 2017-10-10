package li.lingfeng.ltweaks.xposed.communication;

import android.app.Activity;
import android.os.Bundle;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/6/19.
 */
@XposedLoad(packages = {
        PackageNames.QQ_LITE,
        PackageNames.QQ,
        PackageNames.QQ_INTERNATIONAL,
        PackageNames.TIM
}, prefs = R.string.key_qq_dismiss_contacts_enabler)
public class XposedQQContacts extends XposedBase {

    private static final String sPhoneFrameActivity = "com.tencent.mobileqq.activity.phone.PhoneFrameActivity";
    private static final String sPhoneLaunchActivity = "com.tencent.mobileqq.activity.phone.PhoneLaunchActivity";

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookActivity(sPhoneFrameActivity, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("Dismiss contacts enabler.");
                ((Activity) param.thisObject).finish();
            }
        });
        findAndHookActivity(sPhoneLaunchActivity, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("Dismiss contacts enabler.");
                ((Activity) param.thisObject).finish();
            }
        });
    }
}
