package li.lingfeng.ltweaks.xposed.system;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/8/15.
 */

public abstract class XposedAppInfo extends XposedBase {

    private static final String INSTALLED_APP_DETAILS = "com.android.settings.applications.InstalledAppDetails";

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(INSTALLED_APP_DETAILS, "onCreateOptionsMenu", Menu.class, MenuInflater.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String name = newMenuName();
                Logger.i("New menu " + name);
                Menu menu = (Menu) param.args[0];
                menu.add(name);
            }
        });

        findAndHookMethod(INSTALLED_APP_DETAILS, "onOptionsItemSelected", MenuItem.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                MenuItem item = (MenuItem) param.args[0];
                if (!newMenuName().equals(item.getTitle())) {
                    return;
                }

                Activity activity = (Activity) XposedHelpers.callMethod(param.thisObject, "getActivity");
                String packageName = (String) XposedHelpers.getObjectField(param.thisObject, "mPackageName");
                Logger.i("Menu " + newMenuName() + " click, package name " + packageName);
                menuItemSelected(activity, packageName);
                param.setResult(true);
            }
        });
    }

    protected abstract String newMenuName();
    protected abstract void menuItemSelected(Activity activity, String packageName) throws Throwable;
}
