package li.lingfeng.ltweaks.xposed.entertainment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.ViewUtils;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2018/4/14.
 */
@XposedLoad(packages = {
        PackageNames.BILIBILI,
        PackageNames.BILIBILI_IN
}, prefs = R.string.key_bilibili_expand_desc)
public class XposedBilibiliExpandDesc extends XposedBase {

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
                            final TextView desc = (TextView) ViewUtils.findViewByName(rootView, "desc");
                            if (desc != null && !desc.getText().toString().isEmpty()) {
                                new Handler().post(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            handleDesc(rootView, desc);
                                        } catch (Throwable e) {
                                            Logger.e("handleDesc error, " + e);
                                        }
                                    }
                                });
                                end = true;
                            }
                        } catch (Throwable e) {
                            Logger.e("Find desc error, " + e);
                            end = true;
                        } finally {
                            if (end) {
                                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            }
                        }
                    }
                });
            }
        });
    }

    private void handleDesc(ViewGroup rootView, final TextView desc) {
        Logger.i("Expand desc.");
        desc.performClick();
        Object listenerInfo = XposedHelpers.callMethod(desc, "getListenerInfo");
        final View.OnClickListener listener = (View.OnClickListener) XposedHelpers.getObjectField(listenerInfo, "mOnClickListener");

        ViewGroup parent = (ViewGroup) desc.getParent();
        ViewUtils.traverseViews(parent, new ViewUtils.ViewTraverseCallback2() {
            @Override
            public boolean onView(View view, int deep) {
                view.setOnClickListener(null);
                return false;
            }
        });
        parent.setOnClickListener(null);

        View arrow = ViewUtils.findViewByName(parent, "arrow");
        arrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.i("Expand desc by arrow.");
                desc.setOnClickListener(listener);
                desc.performClick();
                desc.setOnClickListener(null);
            }
        });

        // Set desc color to light a bit.
        desc.setTextColor(Color.parseColor("#A3A3A3"));
    }
}
