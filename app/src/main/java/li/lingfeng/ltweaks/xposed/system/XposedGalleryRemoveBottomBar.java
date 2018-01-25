package li.lingfeng.ltweaks.xposed.system;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
 * Created by smallville on 2018/1/24.
 */
@XposedLoad(packages = PackageNames.GALLERY, prefs = R.string.key_lineage_os_gallery_remove_bottom_bar)
public class XposedGalleryRemoveBottomBar extends XposedBase {

    private static final String GALLERY_ACTIVITY = "com.android.gallery3d.app.GalleryActivity";
    private SimpleDrawer mDrawer;
    private ViewGroup mBottomNav;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookActivity(GALLERY_ACTIVITY, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final Activity activity = (Activity) param.thisObject;
                final ViewGroup rootView = (ViewGroup) activity.findViewById(android.R.id.content);
                hookBottomBar(activity, rootView);

                rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mBottomNav != null && mBottomNav.getVisibility() == View.VISIBLE) {
                            mBottomNav.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });

        findAndHookActivity(GALLERY_ACTIVITY, "onBackPressed", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mDrawer != null && mDrawer.isDrawerOpen(Gravity.LEFT)) {
                    Logger.i("Back is pressed for closing drawer.");
                    mDrawer.closeDrawers();
                    param.setResult(null);
                }
            }
        });

        findAndHookActivity(GALLERY_ACTIVITY, "onDestroy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mDrawer = null;
                mBottomNav = null;
            }
        });
    }

    private void hookBottomBar(Activity activity, ViewGroup rootView) throws Throwable {
        Class cls = findClass(ClassNames.BOTTOM_NAV_VIEW);
        mBottomNav = (ViewGroup) ViewUtils.findViewByType(rootView, cls);
        List<FrameLayout> layouts = ViewUtils.findAllViewByTypeInSameHierarchy(mBottomNav, FrameLayout.class, 3);
        Logger.i("Got " + layouts.size() + " bottom buttons.");

        SimpleDrawer.NavItem[] navItems = new SimpleDrawer.NavItem[layouts.size()];
        for (int i = 0; i < navItems.length; ++i) {
            FrameLayout layout = layouts.get(i);
            TextView textView = ViewUtils.findViewByType(layout, TextView.class);
            ImageView imageView = ViewUtils.findViewByType(layout, ImageView.class);
            SimpleDrawer.NavItem navItem = new SimpleDrawer.NavItem(imageView.getDrawable(), textView.getText(), layout);
            navItems[i] = navItem;
        }
        SimpleDrawer.NavItem headerItem = new SimpleDrawer.NavItem(ContextUtils.getAppIcon(), ContextUtils.getAppName(), null);

        FrameLayout allView = ViewUtils.viewChildsIntoOneLayout(activity, rootView);
        mDrawer = new SimpleDrawer(activity, allView, navItems, headerItem);
        mDrawer.updateHeaderBackground(Color.parseColor("#333333"));
        rootView.addView(mDrawer, new WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mBottomNav.setVisibility(View.GONE);
    }
}
