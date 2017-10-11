package li.lingfeng.ltweaks.xposed.shopping;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/10/11.
 */
@XposedLoad(packages = PackageNames.JD, prefs = R.string.key_jd_basic_share_activity)
public class XposedJdBasicShare extends XposedBase {
    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookActivity("com.jingdong.app.mall.basic.ShareActivity", "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                Intent intent = activity.getIntent();
                int action = intent.getIntExtra("action", 0);
                if (action != 1) {
                    intent.putExtra("action", 1);
                    Logger.i("ShareActivity action " + action + " -> 1");
                }
            }
        });
    }
}
