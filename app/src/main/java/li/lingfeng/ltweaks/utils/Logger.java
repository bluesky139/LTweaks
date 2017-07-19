package li.lingfeng.ltweaks.utils;

import android.content.Intent;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;

import li.lingfeng.ltweaks.BuildConfig;

/**
 * Created by smallville on 2016/11/23.
 */
public class Logger {
    private final static String TAG = "Xposed";

    public static void v(String msg) {
        Log.v(TAG, msg);
    }

    public static void d(String msg) {
        Log.d(TAG, msg);
    }

    public static void i(String msg) {
        Log.i(TAG, msg);
    }

    public static void w(String msg) {
        Log.w(TAG, msg);
    }

    public static void e(String msg) {
        Log.e(TAG, msg);
    }

    public static void stackTrace() {
        stackTrace(new Exception("print stack"));
    }

    public static void stackTrace(Throwable e) {
        if (e instanceof InvocationTargetException) {
            e = ((InvocationTargetException) e).getTargetException();
        }
        Log.e(TAG, Log.getStackTraceString(e));
    }

    public static void intent(Intent intent) {
        Logger.d(" intent action: " + intent.getAction());
        Logger.d(" intent type: " + intent.getType());
        Logger.d(" intent data: " + intent.getData());
        if (intent.getExtras() != null) {
            for (String key : intent.getExtras().keySet()) {
                Logger.d(" intent extra: " + key + " -> " + intent.getExtras().get(key));
            }
        }
    }

    public static void paramArgs(Object[] args) {
        for (Object arg : args) {
            Logger.d(" param arg: " + arg);
        }
    }
}
