package li.lingfeng.ltweaks.xposed.system;

import android.content.Context;
import android.provider.Settings;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2018/1/22.
 * https://www.xda-developers.com/nav-bar-customization-was-hidden-in-stock-nougat-all-along-and-it-never-needed-root/
 * https://github.com/GravityBox/GravityBox/blob/158620e50e53bdc4957289957ca8bc984fb90b1e/src/com/ceco/nougat/gravitybox/ModNavigationBar.java
 */
@XposedLoad(packages = PackageNames.ANDROID_SYSTEM_UI, prefs = R.string.key_nav_bar_input_cursor_control)
public class XposedInputCursorNavControl extends XposedBase {

    private static final String NAV_BAR_VIEW = "com.android.systemui.statusbar.phone.NavigationBarView";
    private boolean mVisible = false;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(NAV_BAR_VIEW, "setDisabledFlags", int.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                updateVisibility(param.thisObject);
            }
        });

        findAndHookMethod(NAV_BAR_VIEW, "setNavigationIconHints", int.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                updateVisibility(param.thisObject);
            }
        });
    }

    private void updateVisibility(Object navBarView) {
        final int iconHints = XposedHelpers.getIntField(navBarView, "mNavigationIconHints");
        final int disabledFlags = XposedHelpers.getIntField(navBarView, "mDisabledFlags");
        final boolean visible = (disabledFlags & 0x01000000 /* STATUS_BAR_DISABLE_RECENT */) == 0
                && (iconHints & (1 << 0) /* NAVIGATION_HINT_BACK_ALT */) != 0;
        if (mVisible == visible) {
            return;
        }

        mVisible = visible;
        Context context = (Context) XposedHelpers.callMethod(navBarView, "getContext");
        if (mVisible) {
            Logger.d("Show nav cursor control.");
            String dpadLeft = "content://li.lingfeng.ltweaks.resourceProvider/raw/nav_dpad_left";
            String dpadRight = "content://li.lingfeng.ltweaks.resourceProvider/raw/nav_dpad_right";
            Settings.Secure.putString(context.getContentResolver(), "sysui_nav_bar",
                    "key(21:" + dpadLeft + ")[1.0],back[1.0];home[1.0];recent[1.0],key(22:" + dpadRight + ")[1.0]");
        } else {
            Logger.d("Hide nav cursor control.");
            Settings.Secure.putString(context.getContentResolver(), "sysui_nav_bar", null);
        }
    }
}
