package li.lingfeng.ltweaks.xposed.communication;

import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/6/20.
 */
@XposedLoad(packages = {
        PackageNames.QQ_LITE,
        PackageNames.QQ,
        PackageNames.QQ_INTERNATIONAL,
        PackageNames.TIM
}, prefs = R.string.key_qq_remove_ads)
public class XposedQQAds extends XposedBase {

    private static final String sAdView = "com.tencent.mobileqq.widget.ADView";
    private List<View> mAdViews = new ArrayList<>();

    @Override
    protected void handleLoadPackage() throws Throwable {
        hookAllConstructors(View.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!param.thisObject.getClass().getName().equals(sAdView)) {
                    return;
                }
                View view = (View) param.thisObject;
                if (!mAdViews.contains(view)) {
                    Logger.i("Got AdView " + view);
                    mAdViews.add(view);
                }
            }
        });

        findAndHookMethod(View.class, "onAttachedToWindow", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!param.thisObject.getClass().getName().equals(sAdView)) {
                    return;
                }
                View view = (View) param.thisObject;
                if (!mAdViews.contains(view)) {
                    Logger.i("onAttachedToWindow AdView " + view);
                    mAdViews.add(view);
                }
            }
        });

        findAndHookMethod(View.class, "onDetachedFromWindow", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!param.thisObject.getClass().getName().equals(sAdView)) {
                    return;
                }
                View view = (View) param.thisObject;
                Logger.i("onDetachedFromWindow AdView " + view);
                mAdViews.remove(view);
            }
        });

        findAndHookMethod(View.class, "setVisibility", int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                for (View adview : mAdViews) {
                    if (adview.getParent() != null && adview.getParent() == param.thisObject) {
                        Logger.i("Hide adview " + adview);
                        param.args[0] = View.GONE;
                    }
                }
            }
        });
    }
}
