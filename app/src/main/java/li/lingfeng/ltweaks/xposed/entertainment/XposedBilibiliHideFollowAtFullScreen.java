package li.lingfeng.ltweaks.xposed.entertainment;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.ViewUtils;
import li.lingfeng.ltweaks.xposed.XposedBase;

@XposedLoad(packages = {
        PackageNames.BILIBILI,
        PackageNames.BILIBILI_IN
}, prefs = R.string.key_bilibili_hide_follow_at_full_screen)
public class XposedBilibiliHideFollowAtFullScreen extends XposedBase {

    private static final String VIDEO_DETAILS_ACTIVITY = "tv.danmaku.bili.ui.video.VideoDetailsActivity";

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(VIDEO_DETAILS_ACTIVITY, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final Activity activity = (Activity) param.thisObject;
                final ViewGroup rootView = (ViewGroup) activity.findViewById(android.R.id.content);
                rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        boolean end = false;
                        try {
                            end = hookControllerPage(rootView);
                        } catch (Throwable e) {
                            end = true;
                            Logger.e("Can't hook controller page, " + e);
                            Logger.stackTrace(e);
                        }
                        if (end) {
                            rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
                });
            }
        });
    }

    private boolean hookControllerPage(ViewGroup rootView) {
        final ViewGroup controllerPage = (ViewGroup) ViewUtils.findViewByName(rootView, "controller_page");
        if (controllerPage == null) {
            return false;
        }
        Logger.d("controllerPage " + controllerPage);

        controllerPage.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                View avatarLayout = ViewUtils.findViewByName(controllerPage, "avatar_layout");
                if (avatarLayout != null && avatarLayout.getVisibility() == View.VISIBLE) {
                    Logger.i("Hide follow button at full screen.");
                    avatarLayout.setVisibility(View.GONE);
                }
            }
        });
        return true;
    }
}
