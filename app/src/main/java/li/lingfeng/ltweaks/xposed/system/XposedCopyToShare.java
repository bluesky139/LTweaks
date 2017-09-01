package li.lingfeng.ltweaks.xposed.system;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Handler;

import java.lang.ref.WeakReference;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.ShareUtils;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/8/31.
 */
@XposedLoad(packages = {}, prefs = R.string.key_system_share_copy_to_share)
public class XposedCopyToShare extends XposedBase {

    private WeakReference<Activity> mActivityRef;

    @Override
    protected void handleLoadPackage() throws Throwable {
        if (lpparam.packageName.equals(PackageNames.ANDROID)
                || lpparam.packageName.equals(PackageNames.ANDROID_SYSTEM_UI)) {
            return;
        }

        findAndHookMethod(ClipboardManager.class, "setPrimaryClip", ClipData.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mActivityRef == null) {
                            return;
                        }
                        Activity activity = mActivityRef.get();
                        if (activity != null) {
                            Logger.i("ClipboardManager setPrimaryClip " + param.args[0]);
                            ClipData clipData = (ClipData) param.args[0];
                            ShareUtils.shareClipWithSnackbar(activity, clipData);
                        }
                    }
                }, 500);
            }
        });

        findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                mActivityRef = new WeakReference<>(activity);
            }
        });

        findAndHookMethod(Activity.class, "onPause", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mActivityRef == null) {
                    return;
                }
                Activity activity = (Activity) param.thisObject;
                if (mActivityRef.get() == activity) {
                    mActivityRef = null;
                }
            }
        });
    }
}
