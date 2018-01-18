package li.lingfeng.ltweaks.xposed.communication;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import org.apache.commons.lang3.StringUtils;

import java.io.File;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/9/4.
 */
@XposedLoad(packages = PackageNames.WE_CHAT, prefs = R.string.key_system_share_qrcode_scan)
public class XposedWeChatScanQr extends XposedBase {

    private static final String BASE_SCAN_UI = "com.tencent.mm.plugin.scanner.ui.BaseScanUI";
    private String mScannableImage = null;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookActivity(ClassNames.WE_CHAT_LAUNCHER_UI, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                final Activity activity = (Activity) param.thisObject;
                mScannableImage = activity.getIntent().getStringExtra("ltweaks_scannable_image");
                Logger.i("ltweaks_scannable_image " + mScannableImage);
            }
        });

        findAndHookActivity(ClassNames.WE_CHAT_LAUNCHER_UI, "onNewIntent", Intent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Intent intent = (Intent) param.args[0];
                mScannableImage = intent.getStringExtra("ltweaks_scannable_image");
                Logger.i("ltweaks_scannable_image " + mScannableImage);
            }
        });

        findAndHookActivity(BASE_SCAN_UI, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (StringUtils.isEmpty(mScannableImage)) {
                    return;
                }

                final Activity activity = (Activity) param.thisObject;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            scanImage(activity, mScannableImage);
                        } catch (Throwable e) {
                            Logger.e("scanImage error, " + e);
                            Logger.stackTrace(e);
                        } finally {
                            mScannableImage = null;
                        }
                    }
                }, 1000);
            }
        });
    }

    private void scanImage(Activity activity, String imagePath) throws Throwable {
        Logger.i("scanImage " + imagePath);
        Intent intent = new Intent();
        intent.setData(Uri.fromFile(new File(imagePath)));
        XposedHelpers.callMethod(activity, "onActivityResult", 4660, -1, intent);
    }
}
