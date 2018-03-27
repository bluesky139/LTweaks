package li.lingfeng.ltweaks.xposed.system;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Callback;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.ReflectedGlide;
import li.lingfeng.ltweaks.utils.SimpleDrawer;
import li.lingfeng.ltweaks.utils.SimpleFloatingButton;
import li.lingfeng.ltweaks.utils.ViewUtils;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/2/5.
 */
@XposedLoad(
        packages = PackageNames.COOLAPK,
        prefs = R.string.key_coolapk_remove_bottom_bar,
        loadAtActivityCreate = ClassNames.ACTIVITY)
public class XposedCoolapk extends XposedBase {

    private static final String MAIN_ACTIVITY = "com.coolapk.market.view.main.MainActivity";
    private static final String MAIN_FRAGMENT = "com.coolapk.market.view.main.MainFragment";
    private static final String APP_MANAGER_ACTIVITY = "com.coolapk.market.view.appmanager.AppManagerActivity";
    private static final String THEME_ACTIVITY = "com.coolapk.market.view.theme.ThemeListActivity";
    private static final String SETTINGS_ACTIVITY = "com.coolapk.market.view.settings.SettingsActivity";
    private static final String USER_SPACE_ACTIVITY = "com.coolapk.market.view.user.UserSpaceActivity";
    private static final String FAST_RETURN_VIEW = "com.coolapk.market.widget.FastReturnView";
    private static final String NIGHT_MODE_HELPER = "com.coolapk.market.util.NightModeHelper";
    private static final String APP_HOLDER = "com.coolapk.market.AppHolder";
    private static final String ACTION_MANAGER = "com.coolapk.market.manager.ActionManager";

    private Activity mActivity;
    private ViewGroup mRootView;
    private ViewGroup mContentView;
    private ViewGroup mTabContainer;
    private View mPostButton;
    private View mFastReturnView;
    private SimpleDrawer mDrawerLayout;
    private SimpleFloatingButton mFloatingButton;

    private boolean mIsChangingNightMode = false;

    @Override
    protected void handleLoadPackage() throws Throwable {
        /*findAndHookMethod("android.app.SharedPreferencesImpl", "getBoolean", String.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[0].equals("DISABLE_XPOSED")) {
                    param.setResult(false);
                    Logger.i("Set DISABLE_XPOSED to false.");
                }
            }
        });*/

        findAndHookActivity(MAIN_ACTIVITY, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mActivity = (Activity) param.thisObject;
                mRootView = (ViewGroup) mActivity.findViewById(android.R.id.content);
                final int idContentView = ContextUtils.getIdId("content_view");
                final int idTabContainer = ContextUtils.getIdId("bottom_navigation");
                final int idPostButton = ContextUtils.getIdId("post_button");
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

                            View postButton = mActivity.findViewById(idPostButton);
                            if (mPostButton != postButton) {
                                mPostButton = postButton;
                                mPostButton.setVisibility(View.GONE);
                                Logger.i("Set mPostButton gone.");
                            }

                            if (mFastReturnView != null && mFastReturnView.getParent() != null) {
                                ((ViewGroup) mFastReturnView.getParent()).removeView(mFastReturnView);
                                mFastReturnView = null;
                                Logger.i("Remove mFastReturnView.");
                            }
                        } catch (Throwable e) {
                            Logger.e("onGlobalLayout error, " + e.getMessage());
                            Logger.stackTrace(e);
                        }
                    }
                });

                mFloatingButton = SimpleFloatingButton.make(mActivity);
                Drawable drawable = ContextUtils.getDrawable("ic_add_white_24dp");
                mFloatingButton.setImageDrawable(drawable);
                mFloatingButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mPostButton != null) {
                            Logger.i("Post button perform click.");
                            mPostButton.performClick();
                        }
                    }
                });
                mFloatingButton.show();
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
                mPostButton   = null;
                mFastReturnView = null;
                mDrawerLayout = null;
                if (mFloatingButton != null) {
                    mFloatingButton.destroy();
                    mFloatingButton = null;
                }
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

        findAndHookActivity(MAIN_ACTIVITY, "dispatchTouchEvent", MotionEvent.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (mFloatingButton != null) {
                    mFloatingButton.getActivityTouchEventListener().onDispatch((MotionEvent) param.args[0]);
                }
            }
        });

        findAndHookMethod(ContextThemeWrapper.class, "setTheme", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ContextThemeWrapper themeWrapper = (ContextThemeWrapper) param.thisObject;
                updateColorByTheme(themeWrapper.getTheme());
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

        hookAllConstructors(FAST_RETURN_VIEW, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mFastReturnView = (View) param.thisObject;
                Logger.i("Got mFastReturnView.");
            }
        });

        findAndHookMethod(NIGHT_MODE_HELPER, "shouldChangeNightMode", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mIsChangingNightMode) {
                    Logger.i("shouldChangeNightMode return true.");
                    param.setResult(true);
                    mIsChangingNightMode = false;
                }
            }
        });
    }

    private void handleWithTabContainer(ViewGroup tabContainer) throws Throwable {
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
                if (text.isEmpty()) {
                    Logger.w("Ignore tab " + tabView);
                    continue;
                }
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
                    XposedHelpers.callStaticMethod(findClass(ACTION_MANAGER), "startV8SettingsActivity", mActivity);
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
                    headerItem, true);
            updateColorByTheme(mActivity.getTheme());
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

            // drawer header background
            updateHeaderFilter();

            // header background changed.
            mDrawerLayout.setHeaderBackgroundChangeCallback(new Callback.C0() {
                @Override
                public void onResult() {
                    updateHeaderFilter();
                }
            });
        } else {
            for (int i = tabViews.size() - 1; i >= 0; --i) {
                View tabView = tabViews.get(i);
                int idTabText = ContextUtils.getIdId("bottom_navigation_item_title");
                TextView textView = (TextView) tabView.findViewById(idTabText);
                String text = textView.getText().toString();
                if (text.isEmpty()) {
                    tabViews.remove(i);
                }
            }
            mDrawerLayout.updateClickObjs(tabViews.toArray());
            Logger.i("drawer click views are updated.");
        }
    }

    private void updateColorByTheme(Resources.Theme theme) {
        if (mDrawerLayout != null) {
            int color = ContextUtils.getColorFromTheme(theme, "colorPrimary");
            int listColor = ContextUtils.getColorFromTheme(theme, "mainBackgroundColor");
            int textColor = ContextUtils.getColorFromTheme(theme, android.R.attr.textColorPrimary);
            //if (!(mDrawerLayout.getHeaderLayout().getBackground() instanceof BitmapDrawable)) {
                mDrawerLayout.updateHeaderBackground(color);
            //}
            mDrawerLayout.updateNavListBackground(listColor);
            mDrawerLayout.updateNavListTextColor(textColor);
            Logger.i("Drawer color is updated, " + String.format("%08X", color));

            updateHeaderFilter();
        }
        if (mFloatingButton != null) {
            int color = ContextUtils.getColorFromTheme(theme, "colorPrimary");
            mFloatingButton.setBackgroundColor(color);
        }
    }

    private void updateHeaderFilter() {
        SharedPreferences pref = mActivity.getSharedPreferences("coolapk_preferences_v7", 0);
        String themeName = pref.getString("theme_name", "green");
        boolean isNight = themeName.equals("night");
        if (isNight) {
            mDrawerLayout.getHeaderLayout().getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
            mDrawerLayout.getHeaderImage().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        } else {
            mDrawerLayout.getHeaderLayout().getBackground().clearColorFilter();
            mDrawerLayout.getHeaderImage().clearColorFilter();
        }
    }

    private void switchDayNight() {
        Logger.i("switchDayNight");
        mIsChangingNightMode = true;
        Object appTheme = XposedHelpers.callStaticMethod(findClass(APP_HOLDER), "getAppTheme");
        XposedHelpers.callMethod(appTheme, "checkAutoTheme", mActivity);
    }
}
