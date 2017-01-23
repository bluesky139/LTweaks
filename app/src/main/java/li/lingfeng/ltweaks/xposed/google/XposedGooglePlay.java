package li.lingfeng.ltweaks.xposed.google;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/1/5.
 */
@XposedLoad(packages = "com.android.vending", prefs = R.string.key_google_play_view_in_coolapk)
public class XposedGooglePlay extends XposedBase {

    private MenuItem item;

    private Field fNavigationMgr;
    private Method mGetCurrentDoc;
    private Field fDocv2;
    private Object mLastKnownDoc;

    @Override
    public void handleLoadPackage() throws Throwable {
        findAndHookMethod("com.google.android.finsky.activities.MainActivity", "onCreateOptionsMenu", Menu.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Menu menu = (Menu) param.args[0];
                item = menu.add("View in Coolapk");
                Logger.i("Menu is added, " + item.toString());
            }
        });

        findAndHookMethod("com.google.android.finsky.activities.MainActivity", "onOptionsItemSelected", MenuItem.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (item == param.args[0]) {
                    Logger.d("Menu is clicked .");
                    Object navigationMgr = fNavigationMgr.get(param.thisObject);
                    Object doc = mGetCurrentDoc.invoke(navigationMgr);

                    if (doc == null) {
                        doc = mLastKnownDoc;
                    }

                    Object docv2 = fDocv2.get(doc);
                    Field[] fields = docv2.getClass().getDeclaredFields();
                    Map<String, Integer> stringCount = new HashMap<>();
                    for (Field f : fields) {
                        f.setAccessible(true);
                        if (!Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
                            Logger.d("docv2 str " + f.getName() + " -> " + f.get(docv2));
                            String str = (String) f.get(docv2);
                            if (str == null || str.isEmpty() || str.contains(" ")) {
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

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setPackage("com.coolapk.market");
                    intent.setData(Uri.parse("market://details?id=" + maxStr));
                    ((Activity) param.thisObject).startActivity(intent);
                }
            }
        });

        Class clsMainActivity = lpparam.classLoader.loadClass("com.google.android.finsky.activities.MainActivity");
        Field[] fields = clsMainActivity.getDeclaredFields();
        for (Field f : fields) {
            if (f.getType().getName().startsWith("com.google.android.finsky.navigationmanager.")) {
                fNavigationMgr = f;
                fNavigationMgr.setAccessible(true);
                Logger.i("Got fNavigationMgr.");
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
                Logger.i("Got mGetCurrentDoc.");
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
                Logger.i("Got fDocv2.");
                break;
            }
        }

        if (mGetCurrentDoc != null) {
            findAndHookMethod(fNavigationMgr.getType(), mGetCurrentDoc.getName(), new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    if (param.getResult() != null) {
                        mLastKnownDoc = param.getResult();
                    }
                }
            });
        }
    }
}
