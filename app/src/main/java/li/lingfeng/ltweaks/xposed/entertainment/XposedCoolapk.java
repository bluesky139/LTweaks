package li.lingfeng.ltweaks.xposed.entertainment;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
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
    private List<SharedPreferences> mSharedPrefList = new ArrayList<>();

    private Activity mActivity;
    private ViewGroup mTabContainer;
    private SimpleDrawer mDrawerLayout;
    private boolean mIsDone = false;

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
                final ViewGroup contentView = (ViewGroup) mActivity.findViewById(android.R.id.content);
                Logger.i("Got contentView " + contentView);

                contentView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mTabContainer != null && mTabContainer.getVisibility() == View.VISIBLE) {
                            mTabContainer.setVisibility(View.GONE);
                            Logger.d("Set mTabContainer gone.");
                        }
                        if (!mIsDone) {
                            handleWithContentView(contentView);
                        }
                    }
                });
            }
        });

        findAndHookActivity(MAIN_ACTIVITY, "onDestroy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.d("onDestroy");
                mActivity = null;
                mTabContainer = null;
                mDrawerLayout = null;
                mIsDone = false;
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
    }

    private void handleWithContentView(ViewGroup contentView) {
        mTabContainer = ViewUtils.findViewGroupByName(contentView, "bottom_navigation");
        if (mTabContainer == null) {
            return;
        }

        List<View> tabViews = ViewUtils.findAllViewByName(mTabContainer, "bottom_navigation_container");
        if (tabViews.size() == 0) {
            Logger.e("Can't get bottom_navigation_container");
            mIsDone = true;
            return;
        }

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

        ViewGroup rootView = (ViewGroup) contentView.getParent();
        rootView.removeView(contentView);
        mDrawerLayout = new SimpleDrawer(mActivity, contentView, navItems,
                ContextUtils.getAppIcon(), ContextUtils.getAppName());
        rootView.addView(mDrawerLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        mTabContainer.setVisibility(View.GONE);
        Logger.d("drawer is created.");
        mIsDone = true;
    }
}
