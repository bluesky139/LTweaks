package li.lingfeng.ltweaks.xposed.entertainment;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageParser;
import android.os.Bundle;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;
import li.lingfeng.ltweaks.xposed.XposedCommon;

/**
 * Created by lilingfeng on 2017/6/30.
 */
@XposedLoad(packages = { PackageNames.ANDROID, PackageNames.DOUBAN_MOVIE }, prefs = R.string.key_douban_movie_search)
public class XposedDoubanMovieSearch extends XposedCommon {

    private static final String SEARCH_SUGGESTION_ADAPTER = "com.douban.frodo.search.adapter.SearchSuggestionAdapter";
    private Activity mSearchActivity;

    @Override
    protected void handleLoadPackage() throws Throwable {
        if (lpparam.packageName.equals(PackageNames.ANDROID)) {
            hookAndSetComponentExported(PackageNames.DOUBAN_MOVIE, ClassNames.DOUBAN_MOVIE_SEARCH_ACTIVITY);
        } else {
            hookDouban();
        }
    }

    private void hookDouban() {
        Class clsSearchSuggestionAdapter = findClass(SEARCH_SUGGESTION_ADAPTER);
        Class[] parameterTypes = new Class[] {
                clsSearchSuggestionAdapter, Context.class, String.class, String.class, int.class,
                String.class, boolean.class, String.class, int[].class
        };
        Method[] methodsClick = XposedHelpers.findMethodsByExactParameters(clsSearchSuggestionAdapter, void.class, parameterTypes);
        if (methodsClick.length == 0) {
            return;
        }

        Object[] parameterTypesAndCallback = new Object[parameterTypes.length + 1];
        System.arraycopy(parameterTypes, 0, parameterTypesAndCallback, 0, parameterTypes.length);
        parameterTypesAndCallback[parameterTypes.length] = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[1] instanceof Activity) {
                    Logger.i("SEARCH_SUGGESTION_ADAPTER item click.");
                    mSearchActivity = (Activity) param.args[1];
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mSearchActivity = null;
            }
        };
        findAndHookMethod(clsSearchSuggestionAdapter, methodsClick[0].getName(), parameterTypesAndCallback);

        findAndHookMethod(ContextWrapper.class, "startActivity", Intent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Intent intent = (Intent) param.args[0];
                if (mSearchActivity != null && "douban".equals(intent.getScheme())) {
                    Logger.i("Start douban:// activity.");
                    intent.setFlags(0);
                    mSearchActivity.startActivity(intent);
                    param.setResult(null);
                }
            }
        });
    }
}
