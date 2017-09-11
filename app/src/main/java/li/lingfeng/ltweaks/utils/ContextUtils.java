package li.lingfeng.ltweaks.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Binder;
import android.util.TypedValue;
import android.widget.Toast;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import li.lingfeng.ltweaks.MyApplication;
import li.lingfeng.ltweaks.activities.LoadingDialog;
import li.lingfeng.ltweaks.prefs.PackageNames;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by smallville on 2017/1/25.
 */

public class ContextUtils {

    public static Context createPackageContext(String packageName) {
        try {
            return MyApplication.instance().createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
        } catch (PackageManager.NameNotFoundException e) {
            Logger.e("Can't create context for package " + packageName + ", " + e.getMessage());
            Logger.stackTrace(e);
            return null;
        }
    }

    public static Context createLTweaksContext() {
        return createPackageContext(PackageNames.L_TWEAKS);
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

    public static int getArrayId(String name) {
        return getArrayId(name, MyApplication.instance());
    }

    public static int getArrayId(String name, Context context) {
        return getResId(name, "array", context);
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

    public static String[] getStringArray(String name) {
        return getStringArray(name, MyApplication.instance());
    }

    public static String[] getStringArray(String name, Context context) {
        return context.getResources().getStringArray(getArrayId(name, context));
    }

    public static int[] getIntArrayFromStringArray(String name) {
        return getIntArrayFromStringArray(name, MyApplication.instance());
    }

    public static int[] getIntArrayFromStringArray(String name, Context context) {
        String[] strs = getStringArray(name, context);
        int[] ints = new int[strs.length];
        for (int i = 0; i < strs.length; ++i) {
            ints[i] = Integer.parseInt(strs[i]);
        }
        return ints;
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

    public static int getThemeId(String name) {
        return getThemeId(name, MyApplication.instance());
    }

    public static int getThemeId(String name, Context context) {
        return getResId(name, "style", context);
    }

    public static int getColorFromTheme(Resources.Theme theme, String name) {
        int idColor = getAttrId(name);
        if (idColor <= 0)
            return Color.RED;
        return getColorFromTheme(theme, idColor);
    }

    public static int getColorFromTheme(Resources.Theme theme, int id) {
        TypedValue value = new TypedValue();
        theme.resolveAttribute(id, value, true);
        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return value.data;
        } else {
            return Color.RED;
        }
    }

    public static int getThemeValue(Resources.Theme theme, String name) {
        int id = getAttrId(name);
        if (id <= 0) {
            return 0;
        }
        return getThemeValue(theme, id);
    }

    public static int getThemeValue(Resources.Theme theme, int id) {
        TypedValue value = new TypedValue();
        theme.resolveAttribute(id, value, true);
        return value.data;
    }

    public static int getColorFromStyle(int idStyle, String name) {
        return getColorFromStyle(idStyle, name, MyApplication.instance());
    }

    public static int getColorFromStyle(int idStyle, String name, Context context) {
        int id = getAttrId(name);
        if (id <= 0) {
            return Color.RED;
        }
        return getColorFromStyle(idStyle, id, context);
    }

    public static int getColorFromStyle(int idStyle, int id) {
        return getColorFromStyle(idStyle, id, MyApplication.instance());
    }

    public static int getColorFromStyle(int idStyle, int id, Context context) {
        TypedArray typedArray = context.obtainStyledAttributes(idStyle, new int[] { id });
        return typedArray.getColor(0, Color.RED);
    }

    public static int dp2px(float dpValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue,
                MyApplication.instance().getResources().getDisplayMetrics());
    }

    public static int px2dp(int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

    public static Drawable getAppIcon() {
        return getAppIcon(MyApplication.instance().getPackageName());
    }

    public static Drawable getAppIcon(String packageName) {
        try {
            return MyApplication.instance().getPackageManager().getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            Logger.e("Can't get icon from app " + packageName);
            Logger.stackTrace(e);
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
            Logger.stackTrace(e);
            return "";
        }
    }

    public static boolean isCallingFromLTweaks() {
        try {
            int uid = Binder.getCallingUid();
            ApplicationInfo appInfo = MyApplication.instance().getPackageManager().getApplicationInfo(PackageNames.L_TWEAKS, 0);
            if (uid != appInfo.uid) {
                return true;
            }
        } catch (Exception e) {}
        return false;
    }

    public static String getCallingPackage() {
        try {
            int uid = Binder.getCallingUid();
            return MyApplication.instance().getPackageManager().getNameForUid(uid);
        } catch (Exception e) {}
        return null;
    }

    public static void startBrowser(Context context, String url) {
        Logger.v("startBrowser " + url);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        context.startActivity(intent);
    }

    public static void selectPicture(final Activity activity, final int requestCode) {
        Logger.v("selectPicture with requestCode " + requestCode);
        PermissionUtils.requestPermissions(activity, new PermissionUtils.ResultCallback() {
            @Override
            public void onResult(boolean ok) {
                if (ok) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    activity.startActivityForResult(intent, requestCode);
                }
            }
        }, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    public static void openFolder(Context context, String path) {
        Logger.v("openFolder " + path);
        Uri uri = Uri.parse(path);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "resource/folder");
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "Error.", Toast.LENGTH_SHORT).show();
        }
    }

    public static void openAppInMarket(Activity activity,
                                       String app, // app package name
                                       String market // market package name
    ) throws Throwable {
        if (PackageUtils.isPackageInstalled(market)) {
            Logger.v("Open app in native market " + app + ", " + market);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setPackage(market);
            intent.setData(Uri.parse("market://details?id=" + app));
            activity.startActivity(intent);
        } else {
            openAppInWebMarket(activity, app, market);
        }
    }

    private static void openAppInWebMarket(final Activity activity, String app, String market) throws Throwable {
        Logger.v("Open app in web market " + app + ", " + market);
        if (market.equals(PackageNames.GOOGLE_PLAY)) {
            startBrowser(activity, "https://play.google.com/store/apps/details?id=" + app);
        } else if (market.equals(PackageNames.COOLAPK)) {
            startBrowser(activity, "http://coolapk.com/apk/" + app);
        } else if (market.equals(PackageNames.APKPURE)) {
            getApkPureUrl(activity, app, new Callback.C1<String>() {
                @Override
                public void onResult(String url) {
                    if (url != null) {
                        startBrowser(activity, url);
                    } else {
                        Toast.makeText(activity, "Error.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            throw new Exception("Unknown market in openAppInWebMarket().");
        }
    }

    private static void getApkPureUrl(final Activity activity, final String app, final Callback.C1<String> callback) {
        LoadingDialog.show(activity);
        Request request = new Request.Builder().url("https://m.apkpure.com/search?q=" + app).build();
        new OkHttpClient().newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Logger.e("getApkPureUrl onFailure " + e);
                gotApkPureUrl(activity, null, callback);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200) {
                    String content = response.body().string();
                    Pattern pattern = Pattern.compile("<a class=\"dd\" href=\"(/.+/(.+))\">");
                    Matcher matcher = pattern.matcher(content);
                    while (matcher.find()) {
                        String href = matcher.group(1);
                        String packageName = matcher.group(2);
                        if (packageName.equals(app)) {
                            Logger.i("Got " + href);
                            gotApkPureUrl(activity, "https://m.apkpure.com" + href, callback);
                            return;
                        }
                    }
                } else {
                    Logger.e("getApkPureUrl onResponse " + response);
                }
                gotApkPureUrl(activity, null, callback);
            }
        });
    }

    private static void gotApkPureUrl(Activity activity, final String url, final Callback.C1<String> callback) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                callback.onResult(url);
                LoadingDialog.dismiss();
            }
        });
    }

    public static void searchInMobilism(Activity activity, CharSequence keywords) {
        String url = "https://forum.mobilism.org/search.php?keywords=" + keywords + "&sr=topics&sf=titleonly&fid%5B%5D=398";
        startBrowser(activity, url);
    }

    public static void searchInApkMirror(Activity activity, CharSequence packageName) {
        String url = "https://www.apkmirror.com/?post_type=app_release&searchtype=apk&s=" + packageName;
        startBrowser(activity, url);
    }
}
