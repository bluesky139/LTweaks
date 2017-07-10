package li.lingfeng.ltweaks.xposed.google;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import li.lingfeng.ltweaks.activities.LoadingDialog;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Callback;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.xposed.XposedBase;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by smallville on 2017/1/5.
 */
@XposedLoad(packages = PackageNames.GOOGLE_PLAY, prefs = R.string.key_google_play_view_in_coolapk)
public class XposedGooglePlay extends XposedBase {

    private MenuItem itemCoolApk;
    private MenuItem itemApkPure;
    private MenuItem itemSearchInMobilism;
    private HashMap<MenuItem, String> markets;

    private Field fNavigationMgr;
    private Method mGetCurrentDoc;
    private Field fDocv2;

    @Override
    public void handleLoadPackage() throws Throwable {
        findAndHookMethod("com.google.android.finsky.activities.MainActivity", "onCreateOptionsMenu", Menu.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Menu menu = (Menu) param.args[0];
                itemCoolApk = menu.add("View in CoolApk");
                itemApkPure = menu.add("View in ApkPure");
                itemSearchInMobilism = menu.add("Search in Mobilism");
                markets = new HashMap<MenuItem, String>(2) {{
                    put(itemCoolApk, PackageNames.COOLAPK);
                    put(itemApkPure, PackageNames.APKPURE);
                }};
                Logger.i("Menu is added, View in other market.");
            }
        });

        findAndHookMethod("com.google.android.finsky.activities.MainActivity", "onOptionsItemSelected", MenuItem.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                MenuItem item = (MenuItem) param.args[0];
                if (itemCoolApk != item && itemApkPure != item && itemSearchInMobilism != item) {
                    return;
                }

                Activity activity = (Activity) param.thisObject;
                try {
                    if (itemSearchInMobilism == item) {
                        int idTitle = ContextUtils.getIdId("title_title");
                        TextView titleView = (TextView) activity.findViewById(idTitle);
                        String title = titleView.getText().toString().replaceAll("[^a-zA-Z\\d]", " ").replaceAll("\\s{2,}", " ").trim();
                        Logger.i("title " + title);
                        if (title.isEmpty()) {
                            throw new Exception("Title is empty.");
                        }
                        String url = "https://forum.mobilism.org/search.php?keywords=" + title + "&sr=topics&sf=titleonly&fid%5B%5D=398";
                        ContextUtils.startBrowser(activity, url);
                    } else {
                        Logger.i("Menu is clicked .");
                        Object navigationMgr = fNavigationMgr.get(param.thisObject);
                        Object doc = mGetCurrentDoc.invoke(navigationMgr);

                        Object docv2 = fDocv2.get(doc);
                        Field[] fields = docv2.getClass().getDeclaredFields();
                        Map<String, Integer> stringCount = new HashMap<>();
                        for (Field f : fields) {
                            f.setAccessible(true);
                            if (!Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
                                Logger.d("docv2 str " + f.getName() + " -> " + f.get(docv2));
                                String str = (String) f.get(docv2);
                                if (str == null || str.isEmpty() || str.contains(" ") || !str.contains(".")) {
                                    continue;
                                }
                                if (!stringCount.containsKey(str)) {
                                    stringCount.put(str, 1);
                                } else {
                                    stringCount.put(str, stringCount.get(str) + 1);
                                }
                            }
                        }

                        for (String key : stringCount.keySet()) {
                            Set<String> keys = new HashSet<>(stringCount.keySet());
                            keys.remove(key);
                            for (String k : keys) {
                                if (k.contains(key)) {
                                    stringCount.put(key, stringCount.get(key) + 1);
                                }
                            }
                        }

                        int maxCount = 0;
                        String maxStr = null;
                        for (Map.Entry<String, Integer> kv : stringCount.entrySet()) {
                            Logger.d("count " + kv.getKey() + " " + kv.getValue());
                            if (maxCount < kv.getValue()) {
                                maxCount = kv.getValue();
                                maxStr = kv.getKey();
                            }
                        }
                        Logger.i("Got package name " + maxStr);
                        openAppInMarket(activity, maxStr, markets.get(item));
                    }
                } catch (Exception e) {
                    Logger.e("Can't view in other market, " + e);
                    Logger.stackTrace(e);
                    Toast.makeText(activity, "Error.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Class clsMainActivity = lpparam.classLoader.loadClass("com.google.android.finsky.activities.MainActivity");
        Field[] fields = clsMainActivity.getDeclaredFields();
        for (Field f : fields) {
            if (f.getType().getName().startsWith("com.google.android.finsky.navigationmanager.")) {
                fNavigationMgr = f;
                fNavigationMgr.setAccessible(true);
                Logger.i("Got fNavigationMgr " + f.getType().getName());
                break;
            }
        }

        Method[] methods = fNavigationMgr.getType().getDeclaredMethods();
        Class clsDocument = lpparam.classLoader.loadClass("com.google.android.finsky.dfemodel.Document");
        if (clsDocument == null) {
            clsDocument = lpparam.classLoader.loadClass("com.google.android.finsky.api.model.Document");
        }
        for (Method m : methods) {
            if (Modifier.isPublic(m.getModifiers()) && m.getReturnType() == clsDocument && m.getParameterTypes().length == 0) {
                mGetCurrentDoc = m;
                mGetCurrentDoc.setAccessible(true);
                Logger.i("Got mGetCurrentDoc " + m.getName());
                break;
            }
        }

        fields = clsDocument.getDeclaredFields();
        List<Field> docv2List = new ArrayList<>();
        for (Field f : fields) {
            if (!Modifier.isStatic(f.getModifiers()) && f.getType() != clsDocument && f.getType().getName().startsWith("com.google.android.finsky.") ) {
                docv2List.add(f);
            }
        }

        for (Field docv2Field : docv2List) {
            Class cls = docv2Field.getType();
            fields = cls.getDeclaredFields();
            int stringCount = 0;
            for (Field f : fields) {
                if (!Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
                    ++stringCount;
                }
            }
            if (stringCount > 10) {
                fDocv2 = docv2Field;
                fDocv2.setAccessible(true);
                Logger.i("Got fDocv2 " + fDocv2.getType().getName());
                break;
            }
        }
    }

    private void openAppInMarket(Activity activity, String app, String market) throws Throwable {
        boolean hasMarket = false;
        try {
            ApplicationInfo info = activity.getPackageManager().getApplicationInfo(market, 0);
            hasMarket = info.enabled;
        } catch (PackageManager.NameNotFoundException e) {
        }
        if (hasMarket) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setPackage(market);
            intent.setData(Uri.parse("market://details?id=" + app));
            activity.startActivity(intent);
        } else {
            openAppInWebMarket(activity, app, market);
        }
    }

    private void openAppInWebMarket(final Activity activity, String app, String market) throws Throwable {
        if (market.equals(PackageNames.COOLAPK)) {
            ContextUtils.startBrowser(activity, "http://coolapk.com/apk/" + app);
        } else if (market.equals(PackageNames.APKPURE)) {
            getApkPureUrl(activity, app, new Callback.C1<String>() {
                @Override
                public void onResult(String url) {
                    if (url != null) {
                        ContextUtils.startBrowser(activity, url);
                    } else {
                        Toast.makeText(activity, "Error.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            throw new Exception("Unknown market in openAppInWebMarket().");
        }
    }

    private void getApkPureUrl(final Activity activity, final String app, final Callback.C1<String> callback) {
        LoadingDialog.show(activity);
        Request request = new Request.Builder().url("https://m.apkpure.com/search?q=" + app).build();
        new OkHttpClient().newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Logger.e("getApkPureUrl onFailure " + e);
                gotApkPureUrl(activity, null, callback);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200) {
                    String content = response.body().string();
                    Pattern pattern = Pattern.compile("<a class=\"dd\" href=\"(/.+/(.+))\">");
                    Matcher matcher = pattern.matcher(content);
                    while (matcher.find()) {
                        String href = matcher.group(1);
                        String packageName = matcher.group(2);
                        if (packageName.equals(app)) {
                            Logger.i("Got " + href);
                            gotApkPureUrl(activity, "https://m.apkpure.com" + href, callback);
                            return;
                        }
                    }
                } else {
                    Logger.e("getApkPureUrl onResponse " + response);
                }
                gotApkPureUrl(activity, null, callback);
            }
        });
    }

    private void gotApkPureUrl(Activity activity, final String url, final Callback.C1<String> callback) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                callback.onResult(url);
                LoadingDialog.dismiss();
            }
        });
    }
}
