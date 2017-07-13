package li.lingfeng.ltweaks.xposed.communication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;
import li.lingfeng.ltweaks.xposed.XposedCommon;

/**
 * Created by lilingfeng on 2017/7/12.
 */
@XposedLoad(packages = { PackageNames.ANDROID, PackageNames.ZHI_HU }, prefs = R.string.key_zhihu_finish)
public class XposedZhiHuFinish extends XposedCommon {

    private static final String XPOSED_UTIL = "com.zhihu.android.app.util.XposedUtil";

    @Override
    protected void handleLoadPackage() throws Throwable {
        if (lpparam.packageName.equals(PackageNames.ANDROID)) {
            hookAndSetComponentExported(PackageNames.ZHI_HU, ClassNames.ZHI_HU_MAIN_ACTIVITY);
        } else {
            hookOnBackPressed();
        }
    }

    private void hookOnBackPressed() {
        findAndHookMethod(XPOSED_UTIL, "hasXposed", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });

        findAndHookActivity(ClassNames.ZHI_HU_MAIN_ACTIVITY, "onBackPressed", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                String url = activity.getIntent().getDataString();
                if (url == null || !url.startsWith("http")) {
                    return;
                }

                Object fragment = XposedHelpers.callMethod(param.thisObject, "getCurrentDisplayFragment");
                if (fragment == null) {
                    return;
                }

                Object fragmentManager = XposedHelpers.callMethod(param.thisObject, "getSupportFragmentManager");
                int count = (int) XposedHelpers.callMethod(fragmentManager, "getBackStackEntryCount");
                if (count > 0) {
                    return;
                }

                Object tabItemContainer = XposedHelpers.callMethod(param.thisObject, "getCurrentTabItemContainer");
                Object childFragmentManager = XposedHelpers.callMethod(tabItemContainer, "getChildFragmentManager");
                Object rootFragment = XposedHelpers.callMethod(childFragmentManager, "findFragmentByTag", "host");
                if (!rootFragment.getClass().getSimpleName().equals("FeedsFragment")) {
                    return;
                }

                count = (int) XposedHelpers.callMethod(childFragmentManager, "getBackStackEntryCount");
                if (count == 1) {
                    Logger.i("Finish main activity.");
                    activity.finish();
                    param.setResult(null);
                }
            }
        });
    }
}
