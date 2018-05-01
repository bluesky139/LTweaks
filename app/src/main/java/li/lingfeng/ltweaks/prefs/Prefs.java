package li.lingfeng.ltweaks.prefs;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.crossbowffs.remotepreferences.RemotePreferences;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Set;

import de.robv.android.xposed.XSharedPreferences;
import li.lingfeng.ltweaks.MyApplication;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.utils.Logger;

import static li.lingfeng.ltweaks.prefs.SharedPreferences.ACTION_PREF_CHANGE_PREFIX;

/**
 * Created by smallville on 2016/12/24.
 */

public class Prefs {
    private static final String M_PATH = "/data/data/" + PackageNames.L_TWEAKS + "/shared_prefs/" + PackageNames.L_TWEAKS + "_preferences.xml";
    private static final String N_PATH = "/data/user_de/0/" + PackageNames.L_TWEAKS + "/shared_prefs/" + PackageNames.L_TWEAKS + "_preferences.xml";
    public static final String PATH = Build.VERSION.SDK_INT < Build.VERSION_CODES.N ? M_PATH : N_PATH;
    public static XSharedPreferences zygotePrefs;
    private static boolean sInitedAtActivityCreate = false;

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
        XSharedPreferences pref = new XSharedPreferences(new File(PATH));
        return new SharedPreferences(MyApplication.instance(), pref);
    }

    public static SharedPreferences createZygotePreferences() {
        return new SharedPreferences(MyApplication.instance(), zygotePrefs);
    }

    public static void useZygotePreferences() {
        if (instance_ != null) {
            Logger.w("useZygotePreferences, but instance exists.");
        }
        instance_ = createZygotePreferences();
    }

    public static void clearZygotePreferences() {
        instance_ = null;
    }

    private static SharedPreferences createSharedPreferences() {
        Context context = MyApplication.instance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context = context.createDeviceProtectedStorageContext();
        }
        android.content.SharedPreferences pref = context.getSharedPreferences(
                context.getPackageName() + "_preferences", Context.MODE_WORLD_READABLE);;
        makeWorldReadable();
        return new SharedPreferences(MyApplication.instance(), pref);
    }

    public static void createRemotePreferences(Context appContext) {
        if (instance_ != null) {
            Logger.w("createRemotePreferences, but instance exists.");
        }
        RemotePreferences pref = new RemotePreferences(appContext,
                "li.lingfeng.ltweaks.mainpreferences", "li.lingfeng.ltweaks_preferences");
        instance_ = new SharedPreferences(appContext, pref);
    }

    // Return error string id, or 0 if everything is ok
    public static int initAtActivityCreate(Context context) {
        if (!sInitedAtActivityCreate) {
            if (!checkModeWorldReadable(context)) {
                return R.string.app_error_check_mode_world_readable;
            }
            moveToN();
            listenPreferenceChange();
            sInitedAtActivityCreate = true;
        }
        return 0;
    }

    public static boolean checkModeWorldReadable(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return true;
        }
        try {
            context.createDeviceProtectedStorageContext().getSharedPreferences(
                    "test_preferences_mode", Context.MODE_WORLD_READABLE);
            return true;
        } catch (SecurityException e) {
            Logger.w("LTweaks mode world readable is not hooked.");
            return false;
        }
    }

    // Move settings to world readable place, start from Android 7.0
    private static void moveToN() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }

        // https://github.com/rovo89/XposedBridge/issues/206
        File folder = new File("/data/user_de/0/" + PackageNames.L_TWEAKS);
        folder.setExecutable(true, false);

        final File mFile = new File(M_PATH);
        if (!mFile.exists()) {
            return;
        }
        final File nFile = new File(N_PATH);
        if (nFile.exists()) {
            return;
        }

        try {
            Logger.v("Move exist M prefs to N.");
            FileUtils.copyFile(mFile, nFile);
            nFile.setReadable(true, false);
            mFile.delete();
        } catch (Throwable e) {
            Logger.e("Can't move M prefs to N, " + e);
        }
    }

    private static void listenPreferenceChange() {
        instance().registerOnSharedPreferenceChangeListener(sPreferenceChangeListener);
    }

    private static android.content.SharedPreferences.OnSharedPreferenceChangeListener sPreferenceChangeListener
            = new android.content.SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(android.content.SharedPreferences sharedPreferences, String key) {
            Intent intent = new Intent(ACTION_PREF_CHANGE_PREFIX + key);
            intent.putExtra("key", key);
            Object value = sharedPreferences.getAll().get(key);
            Class valueCls = value.getClass();
            if (valueCls == Boolean.class) {
                intent.putExtra("value", (boolean) value);
            } else if (valueCls == Integer.class) {
                intent.putExtra("value", (int) value);
            } else if (valueCls == Long.class) {
                intent.putExtra("value", (long) value);
            } else if (valueCls == Float.class) {
                intent.putExtra("value", (float) value);
            } else if (valueCls == String.class) {
                intent.putExtra("value", (String) value);
            } else if (Set.class.isAssignableFrom(valueCls)) {
                Set<String> setValue = (Set<String>) value;
                String[] array = new String[setValue.size()];
                intent.putExtra("value", setValue.toArray(array));
            } else {
                Logger.w("Unhandled pref type " + valueCls);
                intent.putExtra("value", value.toString());
            }
            MyApplication.instance().sendBroadcast(intent);
        }
    };

    public static void makeWorldReadable() {
        if (MyApplication.instance() == null) {
            return;
        }
        String packageName = MyApplication.instance().getPackageName();
        if (!packageName.equals(PackageNames.L_TWEAKS)) {
            return;
        }
        try {
            File file = new File(PATH);
            if (file.exists()) {
                file.setReadable(true, false);
            }
        } catch (Throwable e) {}
    }
}
