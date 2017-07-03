package li.lingfeng.ltweaks.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;

import li.lingfeng.ltweaks.MyApplication;
import li.lingfeng.ltweaks.activities.JDHistoryActivity;

/**
 * Created by smallville on 2017/2/1.
 */

public class ComponentUtils {

    public static void enableComponent(String componentCls, boolean enabled) {
        ComponentName componentName = new ComponentName(MyApplication.instance(), componentCls);
        MyApplication.instance().getPackageManager().setComponentEnabledSetting(componentName,
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    public static void enableComponent(Class<?> componentCls, boolean enabled) {
        enableComponent(componentCls.getName(), enabled);
    }

    public static boolean isComponentEnabled(Class<?> componentCls) {
        ComponentName componentName = new ComponentName(MyApplication.instance(), componentCls);
        return MyApplication.instance().getPackageManager().getComponentEnabledSetting(componentName)
                == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    public static boolean isAlias(Activity activity) {
        String name = activity.getIntent().getComponent().getClassName();
        return !name.equals(activity.getClass().getName());
    }

    public static String getAlias(Activity activity) {
        String name = activity.getIntent().getComponent().getClassName();
        return name.substring(activity.getClass().getPackage().getName().length() + 1,
                name.length() - activity.getClass().getSimpleName().length());
    }

    public static String getFullAliasName(Class originalCls, String alias) {
        String[] s = Utils.splitByLastChar(originalCls.getName(), '.');
        return s[0] + "." + alias + s[1];
    }
}
