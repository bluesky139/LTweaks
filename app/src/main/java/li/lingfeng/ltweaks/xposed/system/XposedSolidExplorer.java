package li.lingfeng.ltweaks.xposed.system;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.prefs.Prefs;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/7/7.
 */
@XposedLoad(packages = PackageNames.SOLID_EXPLORER, prefs = {})
public class XposedSolidExplorer extends XposedBase {

    private static final String OPEN_MANAGER = "pl.solidexplorer.files.opening.OpenManager";

    @Override
    protected void handleLoadPackage() throws Throwable {
        hookAllMethods(OPEN_MANAGER, "openInternal", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object seFile = param.args[0];
                Object fileSystem = param.args[1];
                Intent intent = (Intent) param.args[2];
                Logger.i("openInternal " + seFile + ", " + fileSystem);
                if (!fileSystem.getClass().getName().equals("pl.solidexplorer.plugins.network.smb.SMBFileSystem")
                        || !Intent.ACTION_VIEW.equals(intent.getAction())
                        || !intent.getBooleanExtra("streaming", false)) {
                    return;
                }
                Logger.intent(intent);

                Class clsFileSystem = findClass("pl.solidexplorer.filesystem.FileSystem");
                Field fieldDescriptor = XposedHelpers.findField(clsFileSystem, "mDescriptor");
                Object descriptor = fieldDescriptor.get(fileSystem);
                String server = (String) XposedHelpers.callMethod(descriptor, "getServer");
                int port = (int) XposedHelpers.callMethod(descriptor, "getPort");
                String path = (String) XposedHelpers.callMethod(descriptor, "getPath");
                String playingFrom = "smb://" + server + ":" + port + "/" + path;
                Logger.i(playingFrom);

                Set<String> replacers = Prefs.instance().getStringSet(R.string.key_solid_explorer_url_replacers, new HashSet<String>());
                for (String replacer : replacers) {
                    JSONObject jReplacer = (JSONObject) JSON.parse(replacer);
                    String from = jReplacer.getString("from");
                    String to = jReplacer.getString("to");
                    if (from.equals(playingFrom)) {
                        String filePath = (String) XposedHelpers.callMethod(seFile, "getPath");
                        filePath = StringUtils.stripStart(filePath, "/");
                        String[] s = StringUtils.split(filePath, '/');
                        for (int i = 0; i < s.length; ++i) {
                            s[i] = Uri.encode(s[i]);
                        }
                        filePath = StringUtils.join(s, '/');

                        String url = StringUtils.stripEnd(to, "/") + "/" + filePath;
                        Logger.i("url_replace_to " + url);
                        intent.putExtra("url_replace_to", url);
                    }
                }
            }
        });

        findAndHookMethod(OPEN_MANAGER, "openFile", Context.class, Intent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Intent intent = (Intent) param.args[1];
                String url = intent.getStringExtra("url_replace_to");
                if (url != null) {
                    Logger.i("Replace url to " + url);
                    intent.setData(Uri.parse(url));
                    intent.removeExtra("url_replace_to");
                }
            }
        });
    }
}
