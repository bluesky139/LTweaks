package li.lingfeng.ltweaks.xposed.entertainment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.ViewGroup;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.ViewUtils;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by sv on 18-2-12.
 */
@XposedLoad(packages = {
        PackageNames.BILIBILI,
        PackageNames.BILIBILI_IN
}, prefs = R.string.key_bilibili_light_desc_in_night)
public class XposedBilibiliLightDescInNight extends XposedBase {

    private static final String VIDEO_DETAILS_ACTIVITY = "tv.danmaku.bili.ui.video.VideoDetailsActivity";

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookActivity(VIDEO_DETAILS_ACTIVITY, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final Activity activity = (Activity) param.thisObject;
                final ViewGroup rootView = (ViewGroup) activity.findViewById(android.R.id.content);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            hookDescTextColor(rootView);
                        } catch (Throwable e) {
                            Logger.e("Can't set desc text, " + e);
                        }
                    }
                }, 500);
            }
        });
    }

    private void hookDescTextColor(ViewGroup rootView) throws Throwable {
        TextView descView = (TextView) ViewUtils.findViewByName(rootView, "desc");
        descView.setTextColor(Color.parseColor("#A3A3A3"));
    }
}
