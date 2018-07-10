package li.lingfeng.ltweaks.xposed.communication;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2018/7/10.
 */
@XposedLoad(packages = PackageNames.ANDROID, prefs = R.string.key_ttrss_darken)
public class XposedTTRssDarkenStartingWindow extends XposedBase {
    @Override
    protected void handleLoadPackage() throws Throwable {
        hookAllMethods(ClassNames.PHONE_WINDOW, "generateLayout", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Context context = (Context) XposedHelpers.callMethod(param.thisObject, "getContext");
                if (context.getPackageName().equals(PackageNames.TT_RSS)) {
                    ColorDrawable drawable = new ColorDrawable(Color.parseColor("#1c1d1e"));
                    Logger.i("Set night background for phone window.");
                    XposedHelpers.callMethod(param.thisObject, "setBackgroundDrawable", drawable);
                }
            }
        });
    }
}
