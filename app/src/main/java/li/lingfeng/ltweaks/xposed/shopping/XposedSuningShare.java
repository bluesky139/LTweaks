package li.lingfeng.ltweaks.xposed.shopping;

import android.app.Activity;
import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.ShareUtils;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/2/11.
 */
@XposedLoad(packages = PackageNames.SUNING, prefs = R.string.key_suning_share_item)
public class XposedSuningShare extends XposedBase {

    private static final String ITEM_ACTIVITY = "com.suning.mobile.ebuy.commodity.newgoodsdetail.NewGoodsDetailActivity";
    private static final String SHARE_ACTIVITY = "com.suning.mobile.ebuy.base.host.share.main.ShareActivity";
    private Activity mActivity;
    private boolean mIsSharing = false;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(ClipboardManager.class, "setPrimaryClip", ClipData.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (mIsSharing && mActivity != null) {
                    Logger.i("ClipboardManager setPrimaryClip " + param.args[0]);
                    ClipData clipData = (ClipData) param.args[0];
                    ShareUtils.shareClipWithSnackbar(mActivity, clipData);
                }
            }
        });

        findAndHookActivity(ITEM_ACTIVITY, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("Item activity onResume.");
                mActivity = (Activity) param.thisObject;
            }
        });

        findAndHookActivity(ITEM_ACTIVITY, "onStop", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("Item activity onStop.");
                mActivity = null;
            }
        });

        findAndHookActivity(SHARE_ACTIVITY, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("Share activity onResume.");
                mIsSharing = true;
            }
        });

        findAndHookActivity(SHARE_ACTIVITY, "onPause", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("Share activity onPause.");
                mIsSharing = false;
            }
        });
    }
}
