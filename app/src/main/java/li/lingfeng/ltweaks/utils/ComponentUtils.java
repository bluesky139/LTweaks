package li.lingfeng.ltweaks.utils;

import android.content.ComponentName;
import android.content.pm.PackageManager;

import li.lingfeng.ltweaks.MyApplication;
import li.lingfeng.ltweaks.activities.JDHistoryActivity;

/**
 * Created by smallville on 2017/2/1.
 */

public class ComponentUtils {

    public static void enableComponent(Class<?> componentCls, boolean enabled) {
        ComponentName componentName = new ComponentName(MyApplication.instance(), componentCls);
        MyApplication.instance().getPackageManager().setComponentEnabledSetting(componentName,
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
}
