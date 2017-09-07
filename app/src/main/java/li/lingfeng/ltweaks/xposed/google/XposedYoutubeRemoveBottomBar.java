package li.lingfeng.ltweaks.xposed.google;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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
 * Created by lilingfeng on 2017/8/9.
 */
@XposedLoad(packages = PackageNames.YOUTUBE, prefs = R.string.key_youtube_remove_bottom_bar)
public class XposedYoutubeRemoveBottomBar extends XposedBase {

    private static final String MAIN_ACTIVITY = "com.google.android.apps.youtube.app.WatchWhileActivity";
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
                            Logger.e("hookBottomBar error, " + e);
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

        findAndHookMethod(ClassNames.PHONE_WINDOW, "setStatusBarColor", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                int color = (int) param.args[0];
                Logger.d("setStatusBarColor " + color);
                if (mDrawerLayout != null) {
                    mDrawerLayout.updateHeaderBackground(color);
                }
            }
        });
    }

    private void hookBottomBar(final Activity activity) throws Throwable {
        int idPivotBar = ContextUtils.getIdId("pivot_bar_container");
        final ViewGroup pivotBar = (ViewGroup) activity.findViewById(idPivotBar);
        List<LinearLayout> buttons = ViewUtils.findAllViewByTypeInSameHierarchy(pivotBar, LinearLayout.class, 4);
        Logger.d("pivotBar with " + buttons.size() + " buttons.");

        SimpleDrawer.NavItem[] navItems = new SimpleDrawer.NavItem[buttons.size()];
        for (int i = 0; i < buttons.size(); ++i) {
            LinearLayout button = buttons.get(i);
            ImageView imageView = ViewUtils.findViewByType(button, ImageView.class);
            TextView textView = ViewUtils.findViewByType(button, TextView.class);
            SimpleDrawer.NavItem navItem = new SimpleDrawer.NavItem(imageView.getDrawable(), textView.getText(), button);
            navItems[i] = navItem;
        }
        SimpleDrawer.NavItem headerItem = new SimpleDrawer.NavItem(ContextUtils.getAppIcon(),
                ContextUtils.getAppName(), null);

        FrameLayout allView = ViewUtils.rootChildsIntoOneLayout(activity);
        mDrawerLayout = new SimpleDrawer(activity, allView, navItems, headerItem);
        int color = ContextUtils.getColorFromTheme(activity.getTheme(), android.R.attr.colorPrimary);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            color = activity.getWindow().getStatusBarColor();
        }
        mDrawerLayout.updateHeaderBackground(color);
        final ViewGroup rootView = (ViewGroup) activity.findViewById(android.R.id.content);
        rootView.addView(mDrawerLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        Logger.i("Simple drawer is created.");

        int idPaneContainer = ContextUtils.getIdId("pane_fragment_container");
        final View paneContainer = rootView.findViewById(idPaneContainer);
        updatePaneContainerHeight(pivotBar, paneContainer);
        pivotBar.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                Logger.i("pivotBar onLayoutChange.");
                try {
                    List<LinearLayout> buttons = ViewUtils.findAllViewByTypeInSameHierarchy(pivotBar, LinearLayout.class, 4);
                    mDrawerLayout.updateClickObjs(buttons.toArray());
                    updatePaneContainerHeight(pivotBar, paneContainer);
                } catch (Throwable e) {
                    Logger.e("pivotBar onLayoutChange error, " + e);
                }
            }
        });
    }

    private void updatePaneContainerHeight(View pivotBar, View paneContainer) {
        if (paneContainer.getLayoutParams().height > 0) {
            return;
        }

        int pivotBarHeight = pivotBar.getMeasuredHeight();
        int oldPaneHeight = paneContainer.getMeasuredHeight();
        if (pivotBarHeight == 0 || oldPaneHeight == 0) {
            return;
        }

        int newPaneHeight = oldPaneHeight + pivotBarHeight;
        paneContainer.getLayoutParams().height = newPaneHeight;
        pivotBar.getLayoutParams().height = 0;
        Logger.d("pivotBarHeight " + pivotBarHeight + ", paneContainer new height " + newPaneHeight);
    }
}
