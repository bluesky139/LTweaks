package li.lingfeng.ltweaks.xposed.google;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.FinalWrapper;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.SimpleDrawer;
import li.lingfeng.ltweaks.utils.ViewUtils;
import li.lingfeng.ltweaks.xposed.XposedBase;

import static li.lingfeng.ltweaks.utils.ContextUtils.dp2px;

/**
 * Created by lilingfeng on 2018/1/19.
 */
@XposedLoad(packages = PackageNames.GOOGLE_BOOKS, prefs = R.string.key_google_books_remove_bottom_bar)
public class XposedGoogleBooksRemoveBottomBar extends XposedBase {

    private static final String HOME_ACTIVITY = "com.google.android.apps.books.app.HomeActivity";
    private ViewGroup mBottomNav;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookActivity(HOME_ACTIVITY, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                final Activity activity = (Activity) param.thisObject;
                final View rootView = activity.findViewById(android.R.id.content);
                rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mBottomNav == null) {
                            try {
                                hookBottomBar(activity);
                            } catch (Throwable e) {
                                Logger.e("Can't hook bottom bar, " + e);
                                Logger.stackTrace(e);
                            }
                        } else if (mBottomNav.getVisibility() == View.VISIBLE) {
                            Logger.d("Set bottom nav gone.");
                            mBottomNav.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });

        findAndHookActivity(HOME_ACTIVITY, "onDestroy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mBottomNav = null;
            }
        });
    }

    private boolean hookBottomBar(Activity activity) throws Throwable {
        int idBottomNav = ContextUtils.getIdId("bottom_navigation");
        ViewGroup bottomNav = (ViewGroup) activity.findViewById(idBottomNav);
        List<FrameLayout> layouts = ViewUtils.findAllViewByTypeInSameHierarchy(bottomNav, FrameLayout.class, 4);
        Logger.i("Got " + layouts.size() + " bottom buttons.");
        if (layouts.size() == 0) {
            return false;
        }

        final List<SimpleDrawer.NavItem> navItems = new ArrayList<>(layouts.size());
        for (int i = 0; i < layouts.size(); ++i) {
            FrameLayout layout = layouts.get(i);
            ImageView imageView = ViewUtils.findViewByType(layout, ImageView.class);
            if (imageView.getDrawable() == null) {
                return false;
            }
            TextView textView = ViewUtils.findViewByType(layout, TextView.class);
            SimpleDrawer.NavItem navItem = new SimpleDrawer.NavItem(
                    imageView.getDrawable().getConstantState().newDrawable(), textView.getText(), layout);
            navItems.add(navItem);
        }

        int idDrawerList = ContextUtils.getIdId("play_drawer_list");
        ViewGroup drawerList = (ViewGroup) activity.findViewById(idDrawerList);
        if (drawerList == null) {
            return false;
        }

        // "Shop" item in drawer.
        final FinalWrapper<TextView> shopItem = new FinalWrapper<>();
        ViewUtils.traverseViewsByType(drawerList, TextView.class, new ViewUtils.ViewTraverseCallback2() {
            @Override
            public boolean onView(View view, int deep) {
                TextView textView = (TextView) view;
                for (int i = 0; i < navItems.size(); ++i) {
                    if (navItems.get(i).mText.toString().equals(textView.getText().toString())) {
                        shopItem.set(textView);
                        navItems.remove(i);
                        return true;
                    }
                }
                return false;
            }
        }, 0);
        if (shopItem.get() == null) {
            Logger.w("Shop item is not in drawer.");
            return false;
        }

        // Insert list into an empty layout.
        final ViewGroup pivot = (ViewGroup) ViewUtils.prevView(shopItem.get());
        final ListView listView = new ListView(activity);
        final ListViewAdapter listAdapter = new ListViewAdapter(activity, navItems);
        listView.setAdapter(listAdapter);
        listView.setDividerHeight(0);
        pivot.addView(listView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                View one = listAdapter.getView(0, null, listView);
                int height = one.getMeasuredHeight();
                Logger.d("one height " + height);
                pivot.getLayoutParams().height = height * navItems.size();
                pivot.requestLayout();
            }
        });

        bottomNav.setVisibility(View.GONE);
        mBottomNav = bottomNav;
        Logger.i("Bottom buttons are added into drawer.");
        return true;
    }

    class ListViewAdapter extends BaseAdapter {

        private Activity mActivity;
        private List<SimpleDrawer.NavItem> mNavItems;
        private View mDrawer;
        private View[] mViews;

        public ListViewAdapter(Activity activity, List<SimpleDrawer.NavItem> navItems) {
            mActivity = activity;
            mNavItems = navItems;
            mDrawer = activity.findViewById(ContextUtils.getIdId("drawer_container"));
            mViews = new View[navItems.size()];
        }

        @Override
        public int getCount() {
            return mNavItems.size();
        }

        @Override
        public SimpleDrawer.NavItem getItem(int position) {
            return mNavItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = mViews[position];
            if (view != null) {
                return view;
            }

            final SimpleDrawer.NavItem navItem = getItem(position);
            TextView textView = new TextView(mActivity);
            textView.setText(navItem.mText);
            textView.setCompoundDrawablesWithIntrinsicBounds(navItem.mIcon, null, null, null);
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navItem.onClick(v);
                    XposedHelpers.callMethod(mDrawer, "closeDrawers");
                }
            });

            textView.setTextAppearance(mActivity, ContextUtils.getThemeId("PlayDrawerRegularText"));
            textView.setPadding(dp2px(16), dp2px(12), 0, dp2px(12));
            textView.setCompoundDrawablePadding(dp2px(32));
            textView.setSingleLine();
            textView.setGravity(Gravity.CENTER_VERTICAL);

            mViews[position] = textView;
            return textView;
        }
    }
}
