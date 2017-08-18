package li.lingfeng.ltweaks.prefs;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import java.io.File;

import de.robv.android.xposed.XSharedPreferences;
import li.lingfeng.ltweaks.MyApplication;
import li.lingfeng.ltweaks.utils.Logger;

/**
 * Created by smallville on 2016/12/24.
 */

public class Prefs {
    private static SharedPreferences instance_;
    public static SharedPreferences instance() {
        if (instance_ == null) {
            if (MyApplication.instance() == null
                    || !MyApplication.instance().getPackageName().equals(PackageNames.L_TWEAKS)) {
                instance_ = createXSharedPreferences();
            } else {
                instance_ = createSharedPreferences();
            }
        }
        return instance_;
    }

    private static SharedPreferences createXSharedPreferences() {
        XSharedPreferences pref = new XSharedPreferences(PackageNames.L_TWEAKS);
        return new SharedPreferences(pref);
    }

    private static SharedPreferences createSharedPreferences() {
        android.content.SharedPreferences pref = MyApplication.instance().getSharedPreferences(
                MyApplication.instance().getPackageName() + "_preferences",
                Build.VERSION.SDK_INT < Build.VERSION_CODES.N ? Context.MODE_WORLD_READABLE : 0);
        makeWorldReadable();
        return new SharedPreferences(pref);
    }

    public static void makeWorldReadable() {
        if (MyApplication.instance() == null) {
            return;
        }
        String packageName = MyApplication.instance().getPackageName();
        if (!packageName.equals(PackageNames.L_TWEAKS)) {
            return;
        }
        try {
            File file = new File(Environment.getDataDirectory(),
                    "data/" + packageName + "/shared_prefs/" + packageName + "_preferences.xml");
            if (file.exists()) {
                file.setReadable(true, false);
            }
        } catch (Throwable e) {}
    }
}
