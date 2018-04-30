package li.lingfeng.ltweaks.xposed.entertainment;

import android.content.Context;
import android.graphics.drawable.Drawable;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2018/4/27.
 */
@XposedLoad(packages = PackageNames.ANDROID, prefs = R.string.key_bilibili_darken)
public class XposedBilibiliDarkenStartingWindow extends XposedBase {

    @Override
    protected void handleLoadPackage() throws Throwable {
        hookAllMethods(ClassNames.PHONE_WINDOW, "generateLayout", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Context context = (Context) XposedHelpers.callMethod(param.thisObject, "getContext");
                if (context.getPackageName().equals(PackageNames.BILIBILI)
                        || context.getPackageName().equals(PackageNames.BILIBILI_IN)) {
                    Drawable drawable = ContextUtils.getColorDrawable("night", context);
                    if (drawable != null) {
                        Logger.i("Set night background for phone window.");
                        XposedHelpers.callMethod(param.thisObject, "setBackgroundDrawable", drawable);
                    } else {
                        Logger.e("Can't set night backgorund for phone window.");
                    }
                }
            }
        });
    }
}
