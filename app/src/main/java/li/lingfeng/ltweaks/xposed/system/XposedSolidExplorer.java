package li.lingfeng.ltweaks.xposed.system;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/7/7.
 */
@XposedLoad(packages = "pl.solidexplorer2", prefs = {})
public class XposedSolidExplorer extends XposedBase {
    @Override
    protected void handleLoadPackage() throws Throwable {
        hookAllMethods("pl.solidexplorer.files.opening.OpenManager", "openInternal", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Logger.d("openInternal " + param.args[0]);
                Object fileSystem = param.args[1];
                Intent intent = (Intent) param.args[2];
                Logger.d("fileSystem " + fileSystem);
                Logger.intent(intent);
                Class clsFileSystem = findClass("pl.solidexplorer.filesystem.FileSystem");
                Field fieldDescriptor = XposedHelpers.findField(clsFileSystem, "mDescriptor");
                Object descriptor = fieldDescriptor.get(fileSystem);
                Logger.d("descriptor " + descriptor);

                String server = (String) XposedHelpers.callMethod(descriptor, "getServer");
                Logger.d("server " + server);
                int port = (int) XposedHelpers.callMethod(descriptor, "getPort");
                Logger.d("port " + port);
                String path = (String) XposedHelpers.callMethod(descriptor, "getPath");
                Logger.d("path " + path);
            }
        });

        hookAllMethods(Activity.class, "startActivity", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.stackTrace(new Exception("aa"));

                Intent intent = (Intent) param.args[0];
                Logger.intent(intent);
                if (!Intent.ACTION_VIEW.equals(intent.getAction())
                        || !intent.getBooleanExtra("streaming", false)) {
                    return;
                }

                // http://127.0.0.1:57871/public/a.mp4
                String data = intent.getDataString();
                if (data == null) {
                    return;
                }

                Pattern pattern = Pattern.compile("(http://127\\.0\\.0\\.1:\\d+)/(.+)");
                Matcher matcher = pattern.matcher(data);
                if (matcher.find()) {
                    String url = "http://10.10.10.27:81/upload" + "/" + matcher.group(2);
                    Logger.d("url " + url);
                    intent.setData(Uri.parse(url));
                }
            }
        });
    }
}
