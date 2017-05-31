package li.lingfeng.ltweaks.xposed.shopping;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.ShareUtils;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/5/31.
 */

public abstract class XposedShareClip extends XposedBase {

    private Activity mActivity;
    private boolean mIsSharing = false;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(ClipboardManager.class, "setPrimaryClip", ClipData.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (isSharing() && mActivity != null) {
                    Logger.i("ClipboardManager setPrimaryClip " + param.args[0]);
                    ClipData clipData = (ClipData) param.args[0];
                    ShareUtils.shareClipWithSnackbar(mActivity, clipData);
                }
            }
        });

        findAndHookActivity(getItemActivity(), "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("Item activity onResume.");
                mActivity = (Activity) param.thisObject;
            }
        });

        findAndHookActivity(getItemActivity(), "onStop", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("Item activity onStop.");
                mActivity = null;
            }
        });

        if (getShareActivity() != null) {
            findAndHookActivity(getShareActivity(), "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Logger.i("Share activity onResume.");
                    mIsSharing = true;
                }
            });

            findAndHookActivity(getShareActivity(), "onPause", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Logger.i("Share activity onPause.");
                    mIsSharing = false;
                }
            });
        }
    }

    protected abstract String getItemActivity();

    protected String getShareActivity() {
        return null;
    }

    private boolean isSharing() {
        return getShareActivity() == null ? true : mIsSharing;
    }
}
