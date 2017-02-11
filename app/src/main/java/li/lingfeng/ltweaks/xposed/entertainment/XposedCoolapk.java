package li.lingfeng.ltweaks.xposed.entertainment;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.SimpleDrawer;
import li.lingfeng.ltweaks.utils.ViewUtils;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/2/5.
 */
@XposedLoad(packages = PackageNames.COOLAPK, prefs = R.string.key_coolapk_remove_bottom_bar)
public class XposedCoolapk extends XposedBase {

    private static final String MAIN_ACTIVITY = "com.coolapk.market.view.main.MainActivity";
    private static final String MAIN_FRAGMENT = "com.coolapk.market.view.main.MainFragment";
    private List<SharedPreferences> mSharedPrefList = new ArrayList<>();

    private Activity mActivity;
    private ViewGroup mContentView;
    private ViewGroup mTabContainer;
    private SimpleDrawer mDrawerLayout;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(ContextWrapper.class, "getSharedPreferences", String.class, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                SharedPreferences sharedPref = (SharedPreferences) param.getResult();
                if (mSharedPrefList.contains(sharedPref))
                    return;
                mSharedPrefList.add(sharedPref);
                Logger.i("Got one shared preference " + sharedPref);

                findAndHookMethod(sharedPref.getClass(), "getBoolean", String.class, boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args[0].equals("DISABLE_XPOSED")) {
                            param.setResult(false);
                            Logger.i("Set DISABLE_XPOSED to false.");
                        }
                    }
                });
            }
        });

        findAndHookActivity(MAIN_ACTIVITY, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mActivity = (Activity) param.thisObject;
                final ViewGroup rootView = (ViewGroup) mActivity.findViewById(android.R.id.content);
                final int idContentView = ContextUtils.getIdId("content_view");
                final int idTabContainer = ContextUtils.getIdId("bottom_navigation");
                rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        try {
                            //Logger.d("layout changed.");
                            if (mContentView == null) {
                                mContentView = (ViewGroup) mActivity.findViewById(idContentView);
                                if (mContentView != null) {
                                    Logger.i("Got contentView");
                                } else {
                                    return;
                                }
                            }

                            ViewGroup tabContainer = (ViewGroup) mActivity.findViewById(idTabContainer);
                            if (tabContainer == null) {
                                return;
                            }

                            if (mTabContainer != tabContainer) {
                                Logger.i("Got tabContainer.");
                                handleWithTabContainer(tabContainer);
                            }

                            if (mTabContainer != null && mTabContainer.getVisibility() == View.VISIBLE) {
                                mTabContainer.setVisibility(View.GONE);
                                Logger.d("Set mTabContainer gone.");
                            }
                        } catch (Exception e) {
                            Logger.e("onGlobalLayout error, " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        findAndHookActivity(MAIN_ACTIVITY, "onDestroy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("onDestroy");
                mActivity     = null;
                mContentView  = null;
                mTabContainer = null;
                mDrawerLayout = null;
            }
        });

        findAndHookActivity(MAIN_ACTIVITY, "onBackPressed", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
                    Logger.i("Back is pressed for closing drawer.");
                    mDrawerLayout.closeDrawers();
                    param.setResult(null);
                }
            }
        });

        findAndHookMethod(ContextThemeWrapper.class, "setTheme", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ContextThemeWrapper themeWrapper = (ContextThemeWrapper) param.thisObject;
                updateDrawerColor(themeWrapper.getTheme());
            }
        });

        findAndHookMethod(ClassNames.TOOLBAR, "setNavigationOnClickListener", View.OnClickListener.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!param.args[0].getClass().getName().startsWith(MAIN_FRAGMENT))
                    return;
                Logger.i("Toolbar setNavigationOnClickListener " + param.args[0]);
                param.args[0] = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mDrawerLayout != null) {
                            mDrawerLayout.openDrawer(Gravity.LEFT);
                        }
                    }
                };
            }
        });
    }

    private void handleWithTabContainer(ViewGroup tabContainer) {
        mTabContainer = tabContainer;
        List<View> tabViews = ViewUtils.findAllViewByName(mTabContainer, "bottom_navigation_container");
        if (tabViews.size() == 0) {
            Logger.e("Can't get bottom_navigation_container");
            return;
        }

        if (mDrawerLayout == null) {
            SimpleDrawer.NavItem[] navItems = new SimpleDrawer.NavItem[tabViews.size()];
            int idTabIcon = ContextUtils.getIdId("bottom_navigation_item_icon");
            int idTabText = ContextUtils.getIdId("bottom_navigation_item_title");
            for (int i = 0; i < tabViews.size(); ++i) {
                View tabView = tabViews.get(i);
                ImageView iconView = (ImageView) tabView.findViewById(idTabIcon);
                Drawable icon = iconView.getDrawable();
                TextView textView = (TextView) tabView.findViewById(idTabText);
                String text = textView.getText().toString();
                navItems[i] = new SimpleDrawer.NavItem(icon, text, tabView);
                Logger.i("Got tab " + text);
            }

            ViewGroup rootView = (ViewGroup) mContentView.getParent();
            rootView.removeView(mContentView);
            mDrawerLayout = new SimpleDrawer(mActivity, mContentView, navItems,
                    mActivity.getResources().getDrawable(android.R.drawable.sym_def_app_icon),
                    "没有底栏的感觉真好~");
            updateDrawerColor(mActivity.getTheme());
            rootView.addView(mDrawerLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            mTabContainer.setVisibility(View.GONE);
            Logger.i("drawer is created.");
        } else {
            View[] views = new View[tabViews.size()];
            mDrawerLayout.updateClickViews(tabViews.toArray(views));
            Logger.i("drawer click views are updated.");
        }
    }

    private void updateDrawerColor(Resources.Theme theme) {
        if (mDrawerLayout != null) {
            int color = ContextUtils.getColorFromTheme(theme, "colorPrimary");
            int listColor = ContextUtils.getColorFromTheme(theme, "mainBackgroundColor");
            int textColor = ContextUtils.getColorFromTheme(theme, android.R.attr.textColorPrimary);
            mDrawerLayout.updateDrawerColor(color, listColor, textColor);
            Logger.i("Drawer color is updated, " + String.format("%08X", color));
        }
    }
}
