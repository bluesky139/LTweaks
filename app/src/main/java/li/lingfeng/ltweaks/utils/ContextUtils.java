package li.lingfeng.ltweaks.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import li.lingfeng.ltweaks.MyApplication;
import li.lingfeng.ltweaks.prefs.PackageNames;

/**
 * Created by smallville on 2017/1/25.
 */

public class ContextUtils {

    public static Context createPackageContext(String packageName) {
        try {
            return MyApplication.instance().createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
        } catch (PackageManager.NameNotFoundException e) {
            Logger.e("Can't create context for package " + packageName + ", " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static String getResNameById(int id) {
        return getResNameById(id, MyApplication.instance());
    }

    public static String getResNameById(int id, Context context) {
        if (id < 0x7F000000)
            return "";
        try {
            return context.getResources().getResourceEntryName(id);
        } catch (Exception e) {
            return "";
        }
    }

    public static int getResId(String name, String type) {
        return getResId(name, type, MyApplication.instance());
    }

    public static int getResId(String name, String type, Context context) {
        return context.getResources().getIdentifier(name, type, context.getPackageName());
    }

    public static int getIdId(String name) {
        return getIdId(name, MyApplication.instance());
    }

    public static int getIdId(String name, Context context) {
        return getResId(name, "id", context);
    }

    public static int getStringId(String name) {
        return getStringId(name, MyApplication.instance());
    }

    public static int getStringId(String name, Context context) {
        return getResId(name, "string", context);
    }

    public static int getDrawableId(String name) {
        return getDrawableId(name, MyApplication.instance());
    }

    public static int getDrawableId(String name, Context context) {
        return getResId(name, "drawable", context);
    }

    public static int getMipmapId(String name) {
        return getMipmapId(name, MyApplication.instance());
    }

    public static int getMipmapId(String name, Context context) {
        return getResId(name, "mipmap", context);
    }

    public static String getString(String name) {
        return getString(name, MyApplication.instance());
    }

    public static String getString(String name, Context context) {
        return context.getString(getStringId(name, context));
    }

    public static Drawable getDrawable(String name) {
        return getDrawable(name, MyApplication.instance());
    }

    public static Drawable getDrawable(String name, Context context) {
        return context.getResources().getDrawable(getDrawableId(name, context));
    }

    public static Drawable getMipmap(String name) {
        return getMipmap(name, MyApplication.instance());
    }

    public static Drawable getMipmap(String name, Context context) {
        return context.getResources().getDrawable(getMipmapId(name, context));
    }

    public static int getLayoutId(String name) {
        return getLayoutId(name, MyApplication.instance());
    }

    public static int getLayoutId(String name, Context context) {
        return getResId(name, "layout", context);
    }

    public static int getAttrId(String name) {
        return getAttrId(name, MyApplication.instance());
    }

    public static int getAttrId(String name, Context context) {
        return getResId(name, "attr", context);
    }

    public static XmlResourceParser getLayout(String name) {
        return getLayout(name, MyApplication.instance());
    }

    public static XmlResourceParser getLayout(String name, Context context) {
        return context.getResources().getLayout(getLayoutId(name, context));
    }

    public static int getColorFromTheme(Resources.Theme theme, String name) {
        int idColor = getAttrId(name);
        if (idColor < 0)
            return 0xFFFFFFFF;
        return getColorFromTheme(theme, idColor);
    }

    public static int getColorFromTheme(Resources.Theme theme, int id) {
        TypedValue value = new TypedValue();
        theme.resolveAttribute(id, value, true);
        return value.data;
    }

    public static int dp2px(float dpValue){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue,
                MyApplication.instance().getResources().getDisplayMetrics());
    }

    public static Drawable getAppIcon() {
        return getAppIcon(MyApplication.instance().getPackageName());
    }

    public static Drawable getAppIcon(String packageName) {
        try {
            return MyApplication.instance().getPackageManager().getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            Logger.e("Can't get icon from app " + packageName);
            e.printStackTrace();
            return new ColorDrawable(Color.WHITE);
        }
    }

    public static String getAppName() {
        return getAppName(MyApplication.instance().getPackageName());
    }

    public static String getAppName(String packageName) {
        try {
            ApplicationInfo appInfo = MyApplication.instance().getPackageManager().getApplicationInfo(packageName, 0);
            return MyApplication.instance().getPackageManager().getApplicationLabel(appInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }
}
