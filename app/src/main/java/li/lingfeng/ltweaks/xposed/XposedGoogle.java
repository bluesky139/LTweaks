package li.lingfeng.ltweaks.xposed;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.utils.Logger;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

/**
 * Created by smallville on 2017/1/19.
 */
@XposedLoad(packages = "com.google.android.googlequicksearchbox", prefs = R.string.key_google_remove_bottom_bar)
public class XposedGoogle implements IXposedHookLoadPackage {

    private Activity mActivity;
    private View mNowTabs;
    private View mFeed;
    private View mUpcoming;
    private ViewGroup mDrawerMenu;
    private View mDrawerLayout;
    private Method mMethodCloseDrawers;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        findAndHookMethod("com.google.android.apps.gsa.searchnow.SearchNowActivity", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Logger.d("SearchNowActivity onCreate.");
                mActivity = (Activity) param.thisObject;
                final int idNowTabs = getResId("now_tabs", "id");

                View rootView = mActivity.findViewById(android.R.id.content);
                rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mNowTabs != null) {
                            if (mNowTabs.getVisibility() != View.GONE) {
                                mNowTabs.setVisibility(View.GONE);
                                Logger.d("Set mNowTabs gone.");
                            }
                            return;
                        }

                        try {
                            View view = mActivity.findViewById(idNowTabs);
                            if (view == null) {
                                return;
                            }

                            mNowTabs = view;
                            Logger.i("Got mNowTabs " + mNowTabs);
                            handleWithNowTabs(lpparam);
                        } catch (Exception e) {
                            Logger.e("Can't handle with mNowTabs, " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        findAndHookMethod("com.google.android.apps.gsa.searchnow.SearchNowActivity", lpparam.classLoader, "onDestroy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.d("SearchNowActivity onDestroy.");
                mActivity = null;
                mNowTabs = null;
                mFeed = null;
                mUpcoming = null;
                mDrawerMenu = null;
                mDrawerLayout = null;
                mMethodCloseDrawers = null;
            }
        });
    }

    private int getResId(String name, String type) {
        return mActivity.getResources().getIdentifier(name, type, "com.google.android.googlequicksearchbox");
    }

    private void handleWithNowTabs(XC_LoadPackage.LoadPackageParam lpparam) throws Exception {
        final int idStreamTab    = getResId("now_stream_tab", "id");
        final int idDrawerMenu   = getResId("drawer_layout", "id");
        final int idDrawerEntry  = getResId("drawer_entry", "layout");
        final int idDrawerImage  = getResId("drawer_image_view", "id");
        final int idDrawerText   = getResId("drawer_text_view", "id");
        final int idFeedIcon     = getResId("lobby_feed_icon", "drawable");
        final int idUpcomingIcon = getResId("lobby_tray_icon", "drawable");
        final int idFeedText     = getResId("now_interests_tab", "string");
        final int idUpcomingText = getResId("now_update_tab", "string");
        final int idDrawerLayout = getResId("navigation_drawer_layout", "id");

        traverseTabs(mNowTabs, idStreamTab, 0);
        if (mFeed == null || mUpcoming == null) {
            Logger.e("Can't get mFeed or mUpcoming.");
            return;
        }

        mDrawerMenu = (ViewGroup) mActivity.findViewById(idDrawerMenu);
        if (mDrawerMenu == null) {
            Logger.e("Can't get mDrawerMenu.");
            return;
        }

        String feedText = mActivity.getString(idFeedText);
        String upcomingText = mActivity.getString(idUpcomingText);
        createDrawerItem(idDrawerEntry, idDrawerImage, idFeedIcon, idDrawerText, feedText, mFeed, 0);
        createDrawerItem(idDrawerEntry, idDrawerImage, idUpcomingIcon, idDrawerText, upcomingText, mUpcoming, 1);
        mNowTabs.setVisibility(View.GONE);

        mDrawerLayout = mActivity.findViewById(idDrawerLayout);
        if (mDrawerLayout == null) {
            Logger.e("Can't get mDrawerLayout.");
            return;
        }

        Class clsDrawerLayout = XposedHelpers.findClass("android.support.v4.widget.DrawerLayout", lpparam.classLoader);
        mMethodCloseDrawers = clsDrawerLayout.getDeclaredMethod("closeDrawers");
    }

    private void traverseTabs(View view, int id, int depth) {
        if (!(view instanceof ViewGroup)) {
            return;
        }

        ViewGroup viewGroup = (ViewGroup) view;
        for (int i = 0; i < viewGroup.getChildCount(); ++i) {
            view = viewGroup.getChildAt(i);
            if (view.getId() == id) {
                if (mFeed == null) {
                    mFeed = view;
                    Logger.i("Got mFeed " + mFeed);
                    continue;
                } else {
                    mUpcoming = view;
                    Logger.i("Got mUpcoming " + mUpcoming);
                }
            }

            if (mFeed != null && mUpcoming != null) {
                return;
            } else {
                traverseTabs(view, id, depth + 1);
            }
        }
    }

    private void createDrawerItem(int idDrawerEntry, int idDrawerImage, int idIcon, int idDrawerText,
                                  final String menuText, final View clickTab, int pos) {
        FrameLayout layout = new FrameLayout(mActivity);
        View drawerEntry = LayoutInflater.from(mActivity).inflate(idDrawerEntry, layout, true);
        ImageView drawerImage = (ImageView) drawerEntry.findViewById(idDrawerImage);
        Drawable icon = mActivity.getResources().getDrawable(idIcon);
        icon.setColorFilter(0xFF7B7B7B, PorterDuff.Mode.SRC_ATOP);
        drawerImage.setImageDrawable(icon);
        TextView drawerText = (TextView) drawerEntry.findViewById(idDrawerText);
        drawerText.setText(menuText);
        mDrawerMenu.addView(layout, pos);

        layout.setClickable(true);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.i(menuText + " is clicked.");
                clickTab.performClick();
                try {
                    mMethodCloseDrawers.invoke(mDrawerLayout);
                } catch (Exception e) {
                    Logger.e("Can't invoke mMethodCloseDrawers.");
                    e.printStackTrace();
                }
            }
        });
        Logger.i(menuText + " item in drawer is created.");
    }
}
