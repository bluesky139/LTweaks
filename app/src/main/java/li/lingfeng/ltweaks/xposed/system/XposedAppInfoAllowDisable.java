package li.lingfeng.ltweaks.xposed.system;

import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.Pair;
import android.widget.Button;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;

/**
 * Created by lilingfeng on 2017/11/29.
 */
@XposedLoad(packages = PackageNames.ANDROID_SETTINGS,
        prefs = R.string.key_app_info_allow_disable,
        loadPrefsInZygote = true)
public class XposedAppInfoAllowDisable extends XposedAppInfo {
    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(INSTALLED_APP_DETAILS, "handleDisableable", Button.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if ((boolean) param.getResult()) {
                    return;
                }

                Logger.i("handleDisableable return true.");
                Button button = (Button) param.args[0];
                button.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);

                ApplicationInfo info = getApplicationInfo(param);
                if (!info.enabled) {
                    button.setText(ContextUtils.getString("enable_text"));
                }
                param.setResult(true);
            }
        });
    }

    @Override
    protected Pair<String, Integer>[] newMenuNames() {
        return null;
    }

    @Override
    protected void menuItemSelected(CharSequence menuName, XC_MethodHook.MethodHookParam param) throws Throwable {
    }
}
