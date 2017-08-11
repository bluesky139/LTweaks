package li.lingfeng.ltweaks.xposed.communication;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import java.io.File;
import java.util.Random;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.MyApplication;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.IOUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/8/10.
 */
@XposedLoad(packages = {
        PackageNames.QQ_LITE,
        PackageNames.QQ,
        PackageNames.QQ_INTERNATIONAL,
        PackageNames.TIM
}, prefs = R.string.key_qq_fix_image_sharing)
public class XposedQQFixImageSharing extends XposedBase {

    private static final String FORWARD_OPERATIONS = "com.tencent.mobileqq.activity.ForwardOperations";
    private static final String SPLASH_ACTIVITY = "com.tencent.mobileqq.activity.SplashActivity";

    @Override
    protected void handleLoadPackage() throws Throwable {
        // Remove temp UriToPath files after 1 day.
        findAndHookActivity(SPLASH_ACTIVITY, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void[] params) {
                        try {
                            File[] files = new File(getCacheDir()).listFiles();
                            for (File file : files) {
                                if (file.lastModified() + 24 * 3600000 < System.currentTimeMillis()) {
                                    Logger.v("Remove UriToPath file " + file.getAbsolutePath());
                                    file.delete();
                                }
                            }
                        } catch (Throwable e) {
                            Logger.e("Can't remove UriToPath file, " + e);
                        }
                        return null;
                    }
                }.execute();
            }
        });

        findAndHookMethodByParameterAndReturnTypes(FORWARD_OPERATIONS, String.class, Uri.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.getResult() != null) {
                    return;
                }
                Uri uri = (Uri) param.args[0];
                String path = getCacheDir() + "/" + System.currentTimeMillis() + "_" + new Random().nextInt();
                Logger.i("UriToPath, " + uri + " -> " + path);
                if (IOUtils.saveUriToFile(uri, path)) {
                    param.setResult(path);
                }
            }
        });
    }

    private String getCacheDir() {
        String dir = MyApplication.instance().getCacheDir().getAbsolutePath() + "/ltweaks_uri_to_path";
        File file = new File(dir);
        if (!file.exists()) {
            file.mkdir();
        }
        return dir;
    }
}
