package li.lingfeng.ltweaks.xposed.communication;

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
 * Created by lilingfeng on 2017/11/28.
 */
@XposedLoad(packages = PackageNames.PUSH_BULLET, prefs = R.string.key_push_bullet_share_to_all)
public class XposedPushBulletShareToAll extends XposedBase {

    private static final String SHARE_ACTIVITY = "com.pushbullet.android.ui.ShareActivity";

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookActivity(SHARE_ACTIVITY, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                Intent intent = activity.getIntent();
                if (!intent.hasExtra("stream_key")) {
                    Logger.i("Share to all devices.");
                    intent.putExtra("stream_key", "all-of-my-devices");
                }
            }
        });
    }
}
