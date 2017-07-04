package li.lingfeng.ltweaks.xposed.google;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

import static li.lingfeng.ltweaks.utils.ContextUtils.getResId;

/**
 * Created by smallville on 2017/1/21.
 */
@XposedLoad(packages = PackageNames.GOOGLE_NEWSSTAND, prefs = R.string.key_google_newsstand_remove_bottom_bar)
public class XposedGoogleNewsstand extends XposedBase {

    private Activity mActivity;
    private View mTabBarLayout;
    private View mTabForYou;
    private View mTabFollowing;
    private View mTabExplore;
    private View mTabSaved;
    private ListView mDrawerList;
    private ListView mTabList;
    private TabListAdapter mTabListAdapter;
    private View mDrawer;
    private Method mMethodCloseDrawers;

    @Override
    public void handleLoadPackage() throws Throwable {
        findAndHookMethod("com.google.apps.dots.android.newsstand.home.HomeActivity", "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("HomeActivity onCreate.");
                mActivity = (Activity) param.thisObject;
                final int idTabBarLayout = getResId("tab_bar_layout", "id");

                View rootView = mActivity.findViewById(android.R.id.content);
                rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mTabBarLayout != null) {
                            if (mTabBarLayout.getVisibility() != View.GONE) {
                                mTabBarLayout.setVisibility(View.GONE);
                                Logger.i("Set mTabBarLayout gone.");
                            }
                            return;
                        }

                        try {
                            View view = mActivity.findViewById(idTabBarLayout);
                            if (view == null) {
                                return;
                            }

                            mTabBarLayout = view;
                            Logger.i("Got mTabBarLayout " + mTabBarLayout);
                            handleWithTabBarLayout();
                        } catch (Exception e) {
                            Logger.e("Can't handle with mTabBarLayout, " + e.getMessage());
                            Logger.stackTrace(e);
                        }
                    }
                });
            }
        });

        findAndHookMethod(Activity.class, "onDestroy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!param.thisObject.getClass().getName().equals("com.google.apps.dots.android.newsstand.home.HomeActivity"))
                    return;
                Logger.i("HomeActivity onDestroy.");
                mActivity           = null;
                mTabBarLayout       = null;
                mTabForYou          = null;
                mTabFollowing       = null;
                mTabExplore         = null;
                mTabSaved           = null;
                mDrawerList         = null;
                mTabList            = null;
                mTabListAdapter     = null;
                mDrawer             = null;
                mMethodCloseDrawers = null;
            }
        });
    }

    private void handleWithTabBarLayout() throws Exception {
        final int idTabForYou    = getResId("tab_for_you", "id");
        final int idTabFollowing = getResId("tab_following", "id");
        final int idTabExplore   = getResId("tab_explore", "id");
        final int idTabSaved     = getResId("tab_saved", "id");
        final int idDrawerList   = getResId("play_drawer_list", "id");
        final int idDrawer       = getResId("drawer", "id");

        mTabForYou    = mTabBarLayout.findViewById(idTabForYou);
        mTabFollowing = mTabBarLayout.findViewById(idTabFollowing);
        mTabExplore   = mTabBarLayout.findViewById(idTabExplore);
        mTabSaved     = mTabBarLayout.findViewById(idTabSaved);

        mDrawerList = (ListView) mActivity.findViewById(idDrawerList);
        mTabList = new ListView(mActivity);
        mTabListAdapter = new TabListAdapter();
        mTabList.setAdapter(mTabListAdapter);
        mTabList.setDividerHeight(0);
        mTabList.setHeaderDividersEnabled(false);
        mTabList.setFooterDividersEnabled(false);

        View one = mTabListAdapter.getView(0, null, mTabList);
        one.measure(0, 0);
        int height = one.getMeasuredHeight();

        ViewGroup emptyView = (ViewGroup) mDrawerList.getChildAt(1);
        emptyView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
        emptyView.addView(mTabList, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                height * mTabListAdapter.getCount()));
        mTabBarLayout.setVisibility(View.GONE);
        Logger.i("mTabList is created.");

        mDrawer = mActivity.findViewById(idDrawer);
        Class<?> clsDrawerLayout = XposedHelpers.findClass("android.support.v4.widget.DrawerLayout", lpparam.classLoader);
        mMethodCloseDrawers = clsDrawerLayout.getDeclaredMethod("closeDrawers");
    }

    private class TabListAdapter extends BaseAdapter implements View.OnClickListener {

        private TextView[] views = new TextView[getCount()];

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (views[position] != null) {
                return views[position];
            }

            String strIdIcon  = "quantum_ic_play_newsstand_grey600_24";
            String strIdTitle = "for_you_title";
            switch (position) {
                case 0:
                    strIdIcon  = "quantum_ic_play_newsstand_grey600_24";
                    strIdTitle = "for_you_title";
                    break;
                case 1:
                    strIdIcon  = "ic_library_grid_icon";
                    strIdTitle = "following_title";
                    break;
                case 2:
                    strIdIcon  = "quantum_ic_explore_grey600_24";
                    strIdTitle = "explore_title";
                    break;
                case 3:
                    strIdIcon  = "quantum_ic_bookmark_grey600_24";
                    strIdTitle = "saved_tab_title";
                    break;
            }

            final int idTextView = getResId("play_drawer_primary_action_regular", "layout");
            TextView view = (TextView) LayoutInflater.from(mActivity).inflate(idTextView, null);

            final int idTitle = getResId(strIdTitle, "string");
            view.setText(mActivity.getString(idTitle));

            final int idIcon = getResId(strIdIcon, "drawable");
            Drawable icon = mActivity.getResources().getDrawable(idIcon);
            icon.setColorFilter(0xFF7B7B7B, PorterDuff.Mode.SRC_ATOP);
            view.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);

            view.setTag(position);
            view.setOnClickListener(this);
            views[position] = view;
            return view;
        }

        @Override
        public void onClick(View v) {
            try {
                int position = (int) v.getTag();
                switch (position) {
                    case 0:
                        mTabForYou.performClick();
                        break;
                    case 1:
                        mTabFollowing.performClick();
                        break;
                    case 2:
                        mTabExplore.performClick();
                        break;
                    case 3:
                        mTabSaved.performClick();
                        break;
                    default:
                        Logger.e("Unknown pos " + position);
                        return;
                }
                mMethodCloseDrawers.invoke(mDrawer);
            } catch (Exception e) {
                Logger.e("Can't invoke closeDrawers(), " + e.getMessage());
                Logger.stackTrace(e);
            }
        }
    }
}
