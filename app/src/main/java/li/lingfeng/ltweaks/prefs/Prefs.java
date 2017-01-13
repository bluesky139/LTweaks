package li.lingfeng.ltweaks.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import de.robv.android.xposed.XSharedPreferences;
import li.lingfeng.ltweaks.MyApplication;

/**
 * Created by smallville on 2016/12/24.
 */

public class Prefs {
    private static SharedPreferences instance_;
    public static SharedPreferences instance() {
        if (instance_ == null) {
            if (MyApplication.instance() == null) {
                instance_ = createXSharedPreferences();
            } else {
                instance_ = createSharedPreferences();
            }
        }
        return instance_;
    }

    private static XSharedPreferences createXSharedPreferences() {
        XSharedPreferences pref = new XSharedPreferences("li.lingfeng.ltweaks");
        //pref.makeWorldReadable();
        pref.reload();
        return pref;
    }

    private static SharedPreferences createSharedPreferences() {
        return MyApplication.instance().getSharedPreferences(MyApplication.instance().getPackageName() + "_preferences", Context.MODE_WORLD_READABLE);
    }
}
