package li.lingfeng.ltweaks.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.crossbowffs.remotepreferences.RemotePreferenceProvider;

import java.lang.reflect.Field;
import java.util.Map;

import li.lingfeng.ltweaks.utils.Logger;

/**
 * Created by lilingfeng on 2017/8/16.
 */

public class MainPreferenceProvider extends RemotePreferenceProvider {

    public MainPreferenceProvider() {
        super("li.lingfeng.ltweaks.mainpreferences", new String[] { "li.lingfeng.ltweaks_preferences" });
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        int mode = Context.MODE_WORLD_READABLE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context = context.createDeviceProtectedStorageContext();
            mode = 0;
        }
        SharedPreferences preferences = context.getSharedPreferences("li.lingfeng.ltweaks_preferences", mode);
        try {
            Field field = RemotePreferenceProvider.class.getDeclaredField("mPreferences");
            field.setAccessible(true);
            Map<String, SharedPreferences> mPreferences = (Map<String, SharedPreferences>) field.get(this);
            mPreferences.put("li.lingfeng.ltweaks_preferences", preferences);
            preferences.registerOnSharedPreferenceChangeListener(this);
            return true;
        } catch (Throwable e) {
            Logger.e("MainPreferenceProvider error, " + e);
            return false;
        }
    }

    @Override
    protected boolean checkAccess(String prefName, String prefKey, boolean write) {
        return !write;
    }
}
