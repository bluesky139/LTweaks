package li.lingfeng.ltweaks.xposed.google;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.DrawerStruct;
import li.lingfeng.ltweaks.utils.FinalWrapper;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.ViewUtils;
import li.lingfeng.ltweaks.xposed.XposedBase;

import static li.lingfeng.ltweaks.utils.ContextUtils.dp2px;
import static li.lingfeng.ltweaks.utils.ContextUtils.getResId;

/**
 * Created by smallville on 2017/1/19.
 */
@XposedLoad(packages = PackageNames.GOOGLE, prefs = R.string.key_google_remove_bottom_bar)
public class XposedGoogle extends XposedBase {

    private static final String MAIN_ACTIVITY = "com.google.android.apps.gsa.searchnow.SearchNowActivity";
    private static final String MAIN_ACTIVITY2 = "com.google.android.apps.gsa.velour.dynamichosts.VelvetThemedDynamicHostActivity";
    private Map<Activity, HandleOneActivity> mActivities = new HashMap<>();

    @Override
    public void handleLoadPackage() throws Throwable {
        hookMainActivity(MAIN_ACTIVITY);
        hookMainActivity(MAIN_ACTIVITY2);

        // This DrawerLayout is from app itself.
        /*findAndHookMethod(ClassNames.DRAWER_LAYOUT, "closeDrawer", int.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(null);
            }
        });*/

        findAndHookMethod(View.class, "setLayoutParams", ViewGroup.LayoutParams.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Class cls = param.args[0].getClass();
                if (cls != DrawerStruct.LayoutParams.class && cls.getName().startsWith("android.support.v4.widget.")) {
                    View view = (View) param.thisObject;
                    if (view.getContext() instanceof Activity) {
                        Activity activity = (Activity) view.getContext();
                        HandleOneActivity handleOneActivity = mActivities.get(activity);
                        if (handleOneActivity != null && handleOneActivity.viewSetLayoutParams(view)) {
                            param.setResult(null);
                        }
                    }
                }
            }
        });

        findAndHookMethod(ClassNames.DRAWER_LAYOUT, "setDrawerLockMode", int.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                //Logger.d("setDrawerLockMode " + param.args[0]);
                param.setResult(null);
            }
        });
    }

    private void hookMainActivity(String activityName) {
        findAndHookActivity(activityName, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("SearchNowActivity onCreate " + param.thisObject.hashCode());
                if (!mActivities.containsKey(param.thisObject)) {
                    Activity activity = (Activity) param.thisObject;
                    mActivities.put(activity, new HandleOneActivity(activity));
                }
            }
        });

        findAndHookActivity(activityName, "onDestroy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("SearchNowActivity onDestroy " + param.thisObject.hashCode());
                Activity activity = (Activity) param.thisObject;
                HandleOneActivity handleOneActivity = mActivities.remove(activity);
                if (handleOneActivity != null) {
                    handleOneActivity.onDestroy();
                }
            }
        });

        findAndHookActivity(activityName, "onBackPressed", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("SearchNowActivity onBackPressed.");
                Activity activity = (Activity) param.thisObject;
                HandleOneActivity handleOneActivity = mActivities.get(activity);
                if (handleOneActivity != null) {
                    if (handleOneActivity.onBackPressed()) {
                        param.setResult(null);
                    }
                }
            }
        });
    }

    private class HandleOneActivity {

        private Activity mActivity;
        private ViewGroup mNowTabs;
        private FinalWrapper<View> mFeed;
        private FinalWrapper<View> mUpcoming;
        private ViewGroup mDrawerMenu;
        private ViewGroup mDrawerLayout;
        private View mNavView;

        HandleOneActivity(final Activity activity) {
            mActivity = activity;
            final int idNowTabs = getResId("lobby_tabs", "id");

            final ViewGroup rootView = (ViewGroup) mActivity.findViewById(android.R.id.content);
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (mNowTabs != null) {
                        View view = mActivity.findViewById(idNowTabs);
                        if (view != null) {
                            mNowTabs = (ViewGroup) view;
                        }
                        if (mNowTabs.getVisibility() != View.GONE) {
                            traverseTabsAgain();
                            mNowTabs.setVisibility(View.GONE);
                            Logger.i("Set mNowTabs gone.");
                        }
                        return;
                    }

                    try {
                        View view = mActivity.findViewById(idNowTabs);
                        if (view == null) {
                            Logger.w("Can't find now tabs.");
                            return;
                        }

                        Logger.i("Got mNowTabs " + view);
                        if (handleWithNowTabs((ViewGroup) view)) {
                            resizeDrawer();
                            mNowTabs = (ViewGroup) view;
                        }
                    } catch (Exception e) {
                        Logger.e("Can't handle with mNowTabs, " + e.getMessage());
                        Logger.stackTrace(e);
                    }
                }
            });
        }

        void onDestroy() {
            mActivity = null;
            mNowTabs = null;
            mFeed = null;
            mUpcoming = null;
            mDrawerMenu = null;
            mDrawerLayout = null;
            mNavView = null;
        }

        boolean onBackPressed() {
            if (mDrawerLayout != null && (boolean) XposedHelpers.callMethod(mDrawerLayout, "isDrawerOpen", Gravity.LEFT)) {
                Logger.i("Back is pressed for closing drawer.");
                XposedHelpers.callMethod(mDrawerLayout, "closeDrawers");
                return true;
            }
            return false;
        }

        boolean viewSetLayoutParams(View view) {
            return view == mNavView;
        }

        private void resizeDrawer() {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    final ViewGroup rootView = (ViewGroup) mActivity.findViewById(android.R.id.content);
                    Class cls = findClass(ClassNames.DRAWER_LAYOUT);
                    mDrawerLayout = (ViewGroup) ViewUtils.findViewByType(rootView, cls);
                    mNavView = mDrawerLayout.getChildAt(1);
                    mNavView.getLayoutParams().width = dp2px(280);
                    mNavView.getLayoutParams().height = mDrawerLayout.getMeasuredHeight();
                    mNavView.requestLayout();
                    Logger.i("Nav view width is resized.");
                }
            }, 500);
        }

        private boolean handleWithNowTabs(ViewGroup nowTabs) throws Exception {
            final int idStreamTab    = getResId("lobby_tab", "id");
            final int idDrawerMenu   = getResId("drawer_layout", "id");
            final int idDrawerEntry  = getResId("drawer_entry", "layout");
            final int idDrawerImage  = getResId("drawer_image_view", "id");
            final int idDrawerText   = getResId("drawer_text_view", "id");
            final int idFeedIcon     = getResId("lobby_feed_icon", "drawable");
            final int idUpcomingIcon = getResId("lobby_tray_icon", "drawable");
            final int idFeedText     = getResId("now_interests_tab", "string");
            final int idUpcomingText = getResId("now_update_tab", "string");

            traverseTabs(nowTabs, idStreamTab, 0);
            if (mFeed == null || mUpcoming == null) {
                Logger.w("Can't get mFeed or mUpcoming.");
                return false;
            }

            mDrawerMenu = (ViewGroup) mActivity.findViewById(idDrawerMenu);
            if (mDrawerMenu == null) {
                Logger.w("Can't get mDrawerMenu.");
                return false;
            }
            Logger.i("Got mDrawerMenu " + mDrawerMenu);

            String feedText = mActivity.getString(idFeedText);
            String upcomingText = mActivity.getString(idUpcomingText);
            createDrawerItem(idDrawerEntry, idDrawerImage, idFeedIcon, idDrawerText, feedText, mFeed, 0);
            createDrawerItem(idDrawerEntry, idDrawerImage, idUpcomingIcon, idDrawerText, upcomingText, mUpcoming, 1);
            nowTabs.setVisibility(View.GONE);
            return true;
        }

        private void traverseTabs(ViewGroup nowTabs, int id, int depth) {
            View feed = nowTabs.getChildAt(0);
            View upcoming = nowTabs.getChildAt(1);
            if (mFeed == null) {
                mFeed = new FinalWrapper<>(feed);
            } else {
                mFeed.set(feed);
            }
            if (mUpcoming == null) {
                mUpcoming = new FinalWrapper<>(upcoming);
            } else {
                mUpcoming.set(upcoming);
            }
        }

        private void traverseTabsAgain() {
            final int idStreamTab = getResId("now_stream_tab", "id");
            mFeed = null;
            mUpcoming = null;
            traverseTabs(mNowTabs, idStreamTab, 0);
        }

        private void createDrawerItem(int idDrawerEntry, int idDrawerImage, int idIcon, int idDrawerText,
                                      final String menuText, final FinalWrapper<View> clickTab, final int pos) {
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
                    clickTab.get().performClick();
                    XposedHelpers.callMethod(mDrawerLayout, "closeDrawers");
                }
            });
            Logger.i(menuText + " item in drawer is created.");
        }
    }
}
