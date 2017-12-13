package li.lingfeng.ltweaks.xposed.system;

import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.Pair;
import android.view.View;
import android.widget.Button;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
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
        findAndHookMethod(INSTALLED_APP_DETAILS, "initUninstallButtons", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Button button = (Button) XposedHelpers.getObjectField(param.thisObject, "mUninstallButton");
                if (button.isEnabled()) {
                    return;
                }

                Logger.i("initUninstallButtons enable uninstall button.");
                button.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
                button.setEnabled(true);
                button.setOnClickListener((View.OnClickListener) param.thisObject);

                ApplicationInfo info = getApplicationInfo(param);
                if (!info.enabled) {
                    button.setText(ContextUtils.getString("enable_text"));
                }
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
