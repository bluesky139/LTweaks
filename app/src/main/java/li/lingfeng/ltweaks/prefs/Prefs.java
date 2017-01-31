package li.lingfeng.ltweaks.prefs;

import android.content.Context;
import android.os.Build;

import de.robv.android.xposed.XSharedPreferences;
import li.lingfeng.ltweaks.MyApplication;

/**
 * Created by smallville on 2016/12/24.
 */

public class Prefs {
    private static SharedPreferences instance_;
    public static SharedPreferences instance() {
        if (instance_ == null) {
            if (MyApplication.instance() == null
                    || !MyApplication.instance().getPackageName().equals("li.lingfeng.ltweaks")) {
                instance_ = createXSharedPreferences();
            } else {
                instance_ = createSharedPreferences();
            }
        }
        return instance_;
    }

    private static SharedPreferences createXSharedPreferences() {
        XSharedPreferences pref = new XSharedPreferences("li.lingfeng.ltweaks");
        pref.reload();
        return new SharedPreferences(pref);
    }

    private static SharedPreferences createSharedPreferences() {
        android.content.SharedPreferences pref = MyApplication.instance().getSharedPreferences(
                MyApplication.instance().getPackageName() + "_preferences",
                Build.VERSION.SDK_INT < Build.VERSION_CODES.N ? Context.MODE_WORLD_READABLE : 0);
        return new SharedPreferences(pref);
    }
}
