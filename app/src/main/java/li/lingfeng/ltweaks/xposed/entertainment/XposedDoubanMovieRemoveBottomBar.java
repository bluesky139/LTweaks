package li.lingfeng.ltweaks.xposed.entertainment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
 * Created by lilingfeng on 2017/7/27.
 */
@XposedLoad(packages = PackageNames.DOUBAN_MOVIE, prefs = R.string.key_douban_movie_remove_bottom_bar)
public class XposedDoubanMovieRemoveBottomBar extends XposedBase {

    private static final String MAIN_ACTIVITY = "com.douban.movie.activity.MainActivity";
    private SimpleDrawer mDrawerLayout;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookActivity(MAIN_ACTIVITY, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final Activity activity = (Activity) param.thisObject;
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            hookBottomBar(activity);
                        } catch (Throwable e) {
                            Logger.e("Can't hookBottomBar.");
                            Logger.stackTrace(e);
                        }
                    }
                });
            }
        });

        findAndHookActivity(MAIN_ACTIVITY, "onDestroy", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
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
    }

    private void hookBottomBar(Activity activity) throws Throwable {
        int idTabStrip = ContextUtils.getIdId("tab_strip");
        ViewGroup tabStrip = (ViewGroup) activity.findViewById(idTabStrip);
        List<RelativeLayout> layouts = ViewUtils.findAllViewByType(tabStrip, RelativeLayout.class);
        Logger.d("tabStrip with " + layouts.size() + " relative layouts.");

        SimpleDrawer.NavItem[] navItems = new SimpleDrawer.NavItem[layouts.size()];
        for (int i = 0; i < layouts.size(); ++i) {
            RelativeLayout layout = layouts.get(i);
            ImageView imageView = ViewUtils.findViewByType(layout, ImageView.class);
            TextView textView = ViewUtils.findViewByType(layout, TextView.class);
            SimpleDrawer.NavItem navItem = new SimpleDrawer.NavItem(imageView.getDrawable(), textView.getText(), layout);
            navItems[i] = navItem;
        }
        SimpleDrawer.NavItem headerItem = new SimpleDrawer.NavItem(ContextUtils.getAppIcon(),
                ContextUtils.getAppName(), null);

        FrameLayout allView = ViewUtils.rootChildsIntoOneLayout(activity);
        mDrawerLayout = new SimpleDrawer(activity, allView, navItems, headerItem);
        mDrawerLayout.updateHeaderBackground(Color.parseColor("#51C061"));
        mDrawerLayout.updateNavListBackground(Color.parseColor("#F2F1EE"));
        mDrawerLayout.updateNavListTextColor(Color.BLACK);
        ViewGroup rootView = (ViewGroup) activity.findViewById(android.R.id.content);
        rootView.addView(mDrawerLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        tabStrip.setVisibility(View.GONE);
        Logger.i("Simple drawer is created.");
    }
}
