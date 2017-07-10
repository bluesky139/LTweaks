package li.lingfeng.ltweaks.xposed.system;

import android.app.Activity;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Binder;
import android.view.View;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
    private static final String FILE_PROVIDER = "pl.solidexplorer.files.FileProvider";
    private static final String FILE_SYSTEM = "pl.solidexplorer.filesystem.FileSystem";
    private static final String STREAMING_SERVICE = "pl.solidexplorer.files.stream.MediaStreamingService";
    private static final String[] PROTOCOLS = { "smb", "dav", "ftp", "sftp" };

    @Override
    protected void handleLoadPackage() throws Throwable {
        hookAllMethods(OPEN_MANAGER, "openInternal", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object seFile = param.args[0];
                Object fileSystem = param.args[1];
                Intent intent = (Intent) param.args[2];
                Logger.i("openInternal " + seFile + ", " + fileSystem);
                String[] s = StringUtils.split(fileSystem.getClass().getName(), '.');
                String protocol = s[s.length - 2];
                if (!ArrayUtils.contains(PROTOCOLS, protocol)
                        || !Intent.ACTION_VIEW.equals(intent.getAction())
                        || !intent.getBooleanExtra("streaming", false)) {
                    return;
                }
                Logger.intent(intent);

                Class clsFileSystem = findClass(FILE_SYSTEM);
                Field fieldDescriptor = XposedHelpers.findField(clsFileSystem, "mDescriptor");
                Object descriptor = fieldDescriptor.get(fileSystem);
                String server = (String) XposedHelpers.callMethod(descriptor, "getServer");
                int port = (int) XposedHelpers.callMethod(descriptor, "getPort");
                String path = (String) XposedHelpers.callMethod(descriptor, "getPath");
                if (path == null) {
                    path = "";
                }
                path = StringUtils.stripStart(path, "/");
                String playingFrom = protocol + "://" + server + (port == 0 ? "" : (":" + port)) + "/" + path;
                Logger.i(playingFrom);

                Set<String> replacers = Prefs.instance().getStringSet(R.string.key_solid_explorer_url_replacers, new HashSet<String>());
                for (String replacer : replacers) {
                    JSONObject jReplacer = (JSONObject) JSON.parse(replacer);
                    String from = jReplacer.getString("from");
                    String to = jReplacer.getString("to");
                    if (from.equals(playingFrom)) {
                        String filePath = (String) XposedHelpers.callMethod(seFile, "getPath");
                        filePath = StringUtils.stripStart(filePath, "/");
                        s = StringUtils.split(filePath, '/');
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
                Logger.d("openFile");
                Logger.intent(intent);
                if (intent.hasExtra("fsdescriptor")) {
                    intent.removeExtra("url_replace_to");
                    return;
                }

                String url = intent.getStringExtra("url_replace_to");
                if (url != null) {
                    Logger.i("Replace url to " + url);
                    intent.setData(Uri.parse(url));
                    intent.removeExtra("url_replace_to");

                    Class clsStreamingService = findClass(STREAMING_SERVICE);
                    Context context = (Context) param.args[0];
                    intent = new Intent(context, clsStreamingService);
                    intent.putExtra("extra_id", 1);
                    context.startService(intent);
                }
            }
        });

        findAndHookMethod(FILE_PROVIDER, "query", Uri.class, String[].class, String.class, String[].class, String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                ContentProvider provider = (ContentProvider) param.thisObject;
                int uid = Binder.getCallingUid();
                ApplicationInfo appInfo = provider.getContext().getPackageManager().getApplicationInfo(PackageNames.L_TWEAKS, 0);
                if (uid != appInfo.uid) {
                    return;
                }

                Uri uri = (Uri) param.args[0];
                if (!uri.getPathSegments().get(0).equals("db")) {
                    return;
                }

                String dbName = uri.getPathSegments().get(1);
                String dbPath = provider.getContext().getDatabasePath(dbName + ".db").getAbsolutePath();
                String table = uri.getPathSegments().get(2);
                String[] columns = (String[]) param.args[1];
                String selection = (String) param.args[2];
                String[] selectionArgs = (String[]) param.args[3];
                String orderBy = (String) param.args[4];
                Logger.i("Query db " + dbName + ", " + dbPath + ", table " + table);

                SQLiteDatabase db = null;
                try {
                    db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
                    Cursor cursor = db.query(table, columns, selection, selectionArgs, null, null, orderBy);
                    param.setResult(cursor);
                } catch (Exception e) {
                    Logger.e("Error to open db, " + e);
                    if (db != null) {
                        db.close();
                    }
                    param.setResult(null);
                }
            }
        });
    }
}
