package li.lingfeng.ltweaks.xposed.system;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.ReflectedGlide;
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
    private static final String APP_MANAGER_ACTIVITY = "com.coolapk.market.view.appmanager.AppManagerActivity";
    private static final String THEME_ACTIVITY = "com.coolapk.market.view.theme.ThemeListActivity";
    private static final String SETTINGS_ACTIVITY = "com.coolapk.market.view.settings.SettingsActivity";
    private static final String CENTER_FRAGMENT = "com.coolapk.market.view.center.CenterFragment";
    private static final String USER_SPACE_ACTIVITY = "com.coolapk.market.view.user.UserSpaceActivity";

    private Activity mActivity;
    private ViewGroup mRootView;
    private ViewGroup mContentView;
    private ViewGroup mTabContainer;
    private SimpleDrawer mDrawerLayout;

    private Object mCenterFragmentB1; // CenterFragment$b$1 in Coolapk v7.3.2

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod("android.app.SharedPreferencesImpl", "getBoolean", String.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[0].equals("DISABLE_XPOSED")) {
                    param.setResult(false);
                    Logger.i("Set DISABLE_XPOSED to false.");
                }
            }
        });

        findAndHookActivity(MAIN_ACTIVITY, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mActivity = (Activity) param.thisObject;
                mRootView = (ViewGroup) mActivity.findViewById(android.R.id.content);
                final int idContentView = ContextUtils.getIdId("content_view");
                final int idTabContainer = ContextUtils.getIdId("bottom_navigation");
                mRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        try {
                            //Logger.v("layout changed.");
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
                                Logger.i("Set mTabContainer gone.");
                            }
                        } catch (Exception e) {
                            Logger.e("onGlobalLayout error, " + e.getMessage());
                            Logger.stackTrace(e);
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
                mRootView     = null;
                mContentView  = null;
                mTabContainer = null;
                mDrawerLayout = null;
                mCenterFragmentB1 = null;
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
            List<SimpleDrawer.NavItem> navItems = new ArrayList<>();

            // 4 bottom buttons
            int idTabIcon = ContextUtils.getIdId("bottom_navigation_item_icon");
            int idTabText = ContextUtils.getIdId("bottom_navigation_item_title");
            for (View tabView : tabViews) {
                ImageView iconView = (ImageView) tabView.findViewById(idTabIcon);
                Drawable icon = iconView.getDrawable();
                TextView textView = (TextView) tabView.findViewById(idTabText);
                String text = textView.getText().toString();
                SimpleDrawer.NavItem navItem = new SimpleDrawer.NavItem(icon, text, tabView);
                navItems.add(navItem);
                Logger.i("Got tab " + text);
            }

            // app manager
            Drawable icon = ContextUtils.getDrawable("ic_arrow_down_bold_circle_outline_white_24dp");
            String text = ContextUtils.getString("title_app_manager");
            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.setClassName(PackageNames.COOLAPK, APP_MANAGER_ACTIVITY);
                    mActivity.startActivity(intent);
                }
            };
            SimpleDrawer.NavItem navItem = new SimpleDrawer.NavItem(icon, text, listener);
            navItems.add(navItem);

            // theme
            icon = ContextUtils.getDrawable("ic_color_lens_white_24dp");
            text = ContextUtils.getString("title_theme");
            listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.setClassName(PackageNames.COOLAPK, THEME_ACTIVITY);
                    mActivity.startActivity(intent);
                }
            };
            navItem = new SimpleDrawer.NavItem(icon, text, listener);
            navItems.add(navItem);

            // night mode
            icon = ContextUtils.getDrawable("ic_star_white_24dp");
            text = ContextUtils.getString("menu_night_mode");
            listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchDayNight();
                }
            };
            navItem = new SimpleDrawer.NavItem(icon, text, listener);
            navItems.add(navItem);

            // settings
            icon = ContextUtils.getDrawable("ic_settings_white_24dp");
            text = ContextUtils.getString("title_settings");
            listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.setClassName(PackageNames.COOLAPK, SETTINGS_ACTIVITY);
                    mActivity.startActivity(intent);
                }
            };
            navItem = new SimpleDrawer.NavItem(icon, text, listener);
            navItems.add(navItem);

            // drawer header
            icon = mActivity.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
            final SharedPreferences pref = mActivity.getSharedPreferences("coolapk_preferences_v7", 0);
            text = pref.getString("username", "没有底栏的感觉真好~");
            listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String uid = pref.getString("uid", "");
                    if (uid.isEmpty())
                        return;
                    Intent intent = new Intent();
                    intent.setClassName(PackageNames.COOLAPK, USER_SPACE_ACTIVITY);
                    intent.putExtra("EXTRA_UID_EXTRA_USERNAME", uid);
                    mActivity.startActivity(intent);
                }
            };
            SimpleDrawer.NavItem headerItem = new SimpleDrawer.NavItem(icon, text, listener);

            // create drawer
            FrameLayout allView = ViewUtils.rootChildsIntoOneLayout(mActivity);
            SimpleDrawer.NavItem[] navItemArray = new SimpleDrawer.NavItem[navItems.size()];
            mDrawerLayout = new SimpleDrawer(mActivity, allView, navItems.toArray(navItemArray),
                    headerItem);
            updateDrawerColor(mActivity.getTheme());
            mRootView.addView(mDrawerLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            mTabContainer.setVisibility(View.GONE);
            Logger.i("drawer is created.");

            // update header image
            String userAvatar = pref.getString("userAvatar", "");
            if (!userAvatar.isEmpty()) {
                try {
                    Logger.i("Updating header image.");
                    ReflectedGlide.with(mActivity, lpparam.classLoader)
                            .load(userAvatar)
                            .diskCacheStrategy("NONE")
                            .placeholder(android.R.drawable.sym_def_app_icon)
                            .into(mDrawerLayout.getHeaderImage());
                } catch (Throwable e) {
                    Logger.e("Failed to update header image, " + e.getMessage());
                    Logger.stackTrace(e);
                }
            }
        } else {
            mDrawerLayout.updateClickObjs(tabViews.toArray());
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

    private void switchDayNight() {
        try {
            if (mCenterFragmentB1 == null) {
                Class<?> clsCenterFragment = findClass(CENTER_FRAGMENT);
                Object centerFragment = clsCenterFragment.newInstance();
                Logger.i("centerFragment is created.");

                Class<?> clsRebindReportingHolder = findClass(ClassNames.REBIND_REPORTING_HOLDER);
                Class<?>[] classes = clsCenterFragment.getDeclaredClasses();
                Class<?> clsB  = null;
                Class<?> clsB1 = null;
                for (Class<?> cls : classes) {
                    if (!clsRebindReportingHolder.isAssignableFrom(cls))
                        continue;
                    for (int i = 1; i <= 5; ++i) {
                        try {
                            Class<?> cls2 = findClass(cls.getName() + "$" + i);
                            if (!CompoundButton.OnCheckedChangeListener.class.isAssignableFrom(cls2))
                                continue;
                            clsB1 = cls2;
                            clsB  = cls;
                            break;
                        } catch (Throwable t) {
                            break;
                        }
                    }
                    if (clsB != null)
                        break;
                }
                if (clsB == null) {
                    Logger.e("Can't find clsB and clsB1.");
                    return;
                }
                Logger.i("Got clsB " + clsB + ", clsB1 " + clsB1);

                Constructor<?> constructorB = clsB.getConstructors()[0];
                constructorB.setAccessible(true);
                View view = LayoutInflater.from(mActivity).inflate(ContextUtils.getLayoutId("main_me_setting"), null, false);
                Object b = constructorB.newInstance(centerFragment, view, null);
                Logger.i("b is created.");

                if (clsB1.getDeclaredConstructors()[0].getParameterTypes().length == 2) {
                    Method methodG = null; // com.coolapk.market.i.g.g() in v7.9.3
                    Method[] methods = clsB.getSuperclass().getDeclaredMethods();
                    for (Method method : methods) {
                        if (method.getReturnType().getName().startsWith("android.databinding.")) {
                            methodG = method;
                            break;
                        }
                    }
                    if (methodG == null) {
                        throw new Exception("Can't find method g.");
                    }
                    Object g = methodG.invoke(b);
                    Logger.d("g " + g);

                    Constructor<?> constructorB1 = clsB1.getDeclaredConstructors()[0];
                    constructorB1.setAccessible(true);
                    mCenterFragmentB1 = constructorB1.newInstance(b, g);
                } else {
                    Constructor<?> constructorB1 = clsB1.getDeclaredConstructors()[0];
                    constructorB1.setAccessible(true);
                    mCenterFragmentB1 = constructorB1.newInstance(b);
                }
                Logger.i("b1 is created.");
            }

            SharedPreferences pref = mActivity.getSharedPreferences("coolapk_preferences_v7", 0);
            String themeName = pref.getString("theme_name", "green");
            boolean isNight = !themeName.equals("night");

            Method method = CompoundButton.OnCheckedChangeListener.class.getDeclaredMethod("onCheckedChanged", CompoundButton.class, boolean.class);
            method.invoke(mCenterFragmentB1, new Switch(mActivity), isNight);
            Logger.i("Switched day/night, " + isNight);
        } catch (Exception e) {
            if (e instanceof InvocationTargetException) {
                e = new Exception(e.getCause());
            }
            Logger.e("Can't switch day/night, " + e);
            Logger.stackTrace(e);
        }
    }
}
