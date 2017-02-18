package li.lingfeng.ltweaks.xposed.shopping;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Build;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.ShareUtils;
import li.lingfeng.ltweaks.utils.SimpleSnackbar;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/2/18.
 */
@XposedLoad(packages = PackageNames.TAOBAO, prefs = R.string.key_taobao_share_item)
public class XposedTaobaoShare extends XposedBase {

    private static final String ITEM_ACTIVITY = "com.taobao.tao.detail.activity.DetailActivity";
    private Activity mActivity;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(ClipboardManager.class, "setPrimaryClip", ClipData.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (mActivity == null)
                    return;
                Logger.i("ClipboardManager setPrimaryClip " + param.args[0]);
                ClipData clipData = (ClipData) param.args[0];
                ShareUtils.shareClipWithSnackbar(mActivity, clipData);
            }
        });

        findAndHookActivity(ITEM_ACTIVITY, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("Item activity onResume.");
                mActivity = (Activity) param.thisObject;
            }
        });

        findAndHookActivity(ITEM_ACTIVITY, "onPause", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("Item activity onPause.");
                mActivity = null;
            }
        });
    }
}
