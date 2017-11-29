package li.lingfeng.ltweaks.xposed.system;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.Utils;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/8/15.
 */

public abstract class XposedAppInfo extends XposedBase {

    protected static final String INSTALLED_APP_DETAILS = "com.android.settings.applications.InstalledAppDetails";
    public static final String SETTINGS_ACTIVITY = "com.android.settings.SettingsActivity";

    @Override
    protected void handleLoadPackage() throws Throwable {
        final Pair[] names = newMenuNames();
        if (names == null || names.length == 0) {
            return;
        }

        findAndHookMethod(INSTALLED_APP_DETAILS, "onCreateOptionsMenu", Menu.class, MenuInflater.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                for (Pair<String, Integer> pair : names) {
                    String name = pair.first;
                    int priority = pair.second;
                    Logger.i("New menu " + name);
                    Menu menu = (Menu) param.args[0];
                    menu.add(Menu.NONE, Menu.NONE, priority, name);
                }
            }
        });

        findAndHookMethod(INSTALLED_APP_DETAILS, "onOptionsItemSelected", MenuItem.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                MenuItem item = (MenuItem) param.args[0];
                if (!Utils.pairContains(names, item.getTitle(), true)) {
                    return;
                }

                Logger.i("Menu " + item.getTitle() + " click.");
                menuItemSelected(item.getTitle(), param);
                param.setResult(true);
            }
        });
    }

    protected Activity getActivity(XC_MethodHook.MethodHookParam param) {
        return (Activity) XposedHelpers.callMethod(param.thisObject, "getActivity");
    }

    protected String getPackageName(XC_MethodHook.MethodHookParam param) {
        return (String) XposedHelpers.getObjectField(param.thisObject, "mPackageName");
    }

    protected ApplicationInfo getApplicationInfo(XC_MethodHook.MethodHookParam param) {
        Object appEntry = XposedHelpers.getObjectField(param.thisObject, "mAppEntry");
        return (ApplicationInfo) XposedHelpers.getObjectField(appEntry, "info");
    }

    protected abstract Pair<String, Integer>[] newMenuNames();
    protected abstract void menuItemSelected(CharSequence menuName, XC_MethodHook.MethodHookParam param) throws Throwable;
}
