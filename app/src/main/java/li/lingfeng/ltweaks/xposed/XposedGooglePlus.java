package li.lingfeng.ltweaks.xposed;

import android.app.Activity;
import android.app.Application;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Arrays;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;

/**
 * Created by smallville on 2017/1/4.
 */
@XposedLoad(packages = "com.google.android.apps.plus", prefs = R.string.key_google_plus_remove_bottom_bar)
public class XposedGooglePlus implements IXposedHookLoadPackage {

    Activity activity;
    View rootView;

    View tabBar;
    int tabBarSpacerId = 0;
    TextView[] tabButtons = new TextView[4];
    View tabBarCounter;

    LinearLayout drawerFragment;
    ListView navList;
    ListView barList;
    BarListAdapter barListAdapter;
    ImageView counter;

    boolean done = false;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        findAndHookMethod("com.google.android.apps.plus.Gplus_Application", lpparam.classLoader, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                Application app = (Application) param.thisObject;
                app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                        if (!activity.getClass().getName().equals("com.google.android.apps.plus.phone.HomeActivity"))
                            return;
                        XposedGooglePlus.this.activity = activity;

                        tabBarSpacerId = activity.getResources().getIdentifier("bottom_navigation_spacer", "id", "com.google.android.apps.plus");
                        if (tabBarSpacerId == 0)
                            return;

                        rootView = activity.findViewById(android.R.id.content);
                        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                //Log.w(TAG, "layout changed.");
                                traverseViewChilds(rootView, 0);
                                if (tabBar != null && tabBar.getVisibility() == View.VISIBLE) {
                                    ViewGroup.LayoutParams tabBarParams = tabBar.getLayoutParams();
                                    tabBarParams.height = 0;
                                    tabBar.setLayoutParams(tabBarParams);
                                    tabBar.setVisibility(View.INVISIBLE);
                                }
                                //if (tabBarCounter != null)
                                //    tabBarCounter.setVisibility(View.VISIBLE);
                                if (counter != null && counter.getVisibility() != tabBarCounter.getVisibility())
                                    counter.setVisibility(tabBarCounter.getVisibility());
                                if (!done && isReady())
                                    createBarList();
                            }
                        });

                        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        //    XposedGooglePlus.this.activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                        //}
                    }

                    @Override
                    public void onActivityStarted(Activity activity) {

                    }

                    @Override
                    public void onActivityResumed(Activity activity) {

                    }

                    @Override
                    public void onActivityPaused(Activity activity) {

                    }

                    @Override
                    public void onActivityStopped(Activity activity) {

                    }

                    @Override
                    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

                    }

                    @Override
                    public void onActivityDestroyed(Activity activity) {
                        if (!activity.getClass().getName().equals("com.google.android.apps.plus.phone.HomeActivity"))
                            return;
                        XposedGooglePlus.this.activity = null;
                        rootView = null;
                        tabBar = null;
                        tabBarSpacerId = 0;
                        Arrays.fill(tabButtons, null);
                        tabBarCounter = null;
                        drawerFragment = null;
                        barList = null;
                        barListAdapter = null;
                        counter = null;
                        done = false;
                    }
                });
            }
        });
    }

    boolean isReady() {
        if (activity != null && tabBar != null &&  drawerFragment != null && navList != null) {
            for (View button : tabButtons) {
                if (button == null)
                    return false;
            }
            return true;
        }
        return false;
    }

    void traverseViewChilds(View view, int depth) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); ++i) {
                view = viewGroup.getChildAt(i);
                //Log.d(TAG, "child view" + depth + " " + view + " id " + view.getId());
                if (tabBar == null) {
                    if (getResNameById(view.getId()).equals("bottom_navigation_container")) {
                        Logger.i("got bottom_navigation_container.");
                        tabBar = view;
                    }
                }
                if (view.getId() > 0 && tabBarSpacerId == view.getId() && view.getVisibility() != View.GONE) {
                    Logger.i("got bottom_navigation_spacer." + view.getVisibility());
                    view.setVisibility(View.GONE);
                }
                if (tabButtons[0] == null && view.getId() > 0) {
                    if (getResNameById(view.getId()).equals("navigation_home")) {
                        Logger.i("got navigation_home.");
                        tabButtons[0] = (TextView) view;
                    }
                }
                if (tabButtons[1] == null && view.getId() > 0) {
                    if (getResNameById(view.getId()).equals("navigation_collections")) {
                        Logger.i("got navigation_collections.");
                        tabButtons[1] = (TextView) view;
                    }
                }
                if (tabButtons[2] == null && view.getId() > 0) {
                    if (getResNameById(view.getId()).equals("navigation_communities")) {
                        Logger.i("got navigation_communities.");
                        tabButtons[2] = (TextView) view;
                    }
                }
                if (tabButtons[3] == null && view.getId() > 0) {
                    if (getResNameById(view.getId()).equals("navigation_notifications_text")) {
                        Logger.i("got navigation_notifications_text.");
                        tabButtons[3] = (TextView) view;
                    }
                }
                if (tabBarCounter == null && view.getId() > 0) {
                    if (getResNameById(view.getId()).equals("navigation_notifications_count")) {
                        Logger.i("got navigation_notifications_count.");
                        tabBarCounter = view;
                    }
                }
                if (drawerFragment == null && view.getId() > 0) {
                    if (getResNameById(view.getId()).equals("menu_items_view")) {
                        Logger.i("got menu_items_view.");
                        drawerFragment = (LinearLayout) view.getParent();
                        navList = (ListView) view;
                    }
                }
                traverseViewChilds(view, depth + 1);
            }
        }
    }

    String getResNameById(int id) {
        if (id < 0x7F000000)
            return "";
        try {
            return activity.getResources().getResourceEntryName(id);
        } catch (Exception e) {
            return "";
        }
    }

    void createBarList() {
        ListAdapter adapter = navList.getAdapter();
        if (adapter != null && adapter.getCount() > 4) {
            int titleId = activity.getResources().getIdentifier("navigation_item_name", "id", "com.google.android.apps.plus");
            View view = adapter.getView(0, null, null).findViewById(titleId);
            if (view != null) {
                TextView title = (TextView) view;
                if (title.getText().toString().equals(tabButtons[0].getText().toString())) {
                    Logger.i("Drawer menu has buttons of bottom bar, skip create.");
                    done = true;
                    return;
                }
            }
        }

        barListAdapter = new BarListAdapter();
        View one = barListAdapter.getView(0, null, barList);
        one.measure(0, 0);
        int height = one.getMeasuredHeight();

        barList = new ListView(XposedGooglePlus.this.activity);
        barList.setLayoutParams(new ListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height * barListAdapter.getCount()));
        barList.setHeaderDividersEnabled(false);
        barList.setFooterDividersEnabled(false);
        barList.setDividerHeight(0);
        barList.setAdapter(barListAdapter);
        //barList.setOnItemClickListener(barListAdapter);
        navList.addHeaderView(barList);
        done = true;
    }

    class BarListAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {

        View[] views = new View[tabButtons.length];

        @Override
        public int getCount() {
            return tabButtons.length;
        }

        @Override
        public Object getItem(int position) {
            return tabButtons[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (views[position] != null)
                return views[position];

            int layoutId = activity.getResources().getIdentifier("navigation_item", "layout", "com.google.android.apps.plus");
            View view = LayoutInflater.from(activity).inflate(layoutId, barList, false);
            int titleId = activity.getResources().getIdentifier("navigation_item_name", "id", "com.google.android.apps.plus");
            TextView title = (TextView) view.findViewById(titleId);
            title.setText(tabButtons[position].getText());

            String strDrwable = null;
            switch (position)
            {
                case 0:
                    strDrwable = "quantum_ic_home_grey600_24";
                    break;
                case 1:
                    strDrwable = "quantum_ic_google_collections_grey600_24";
                    break;
                case 2:
                    strDrwable = "quantum_ic_communities_grey600_24";
                    break;
                case 3:
                    strDrwable = "quantum_ic_notifications_grey600_24";
                    break;
            }
            if (strDrwable != null) {
                int iconDrawableId = activity.getResources().getIdentifier(strDrwable, "drawable", "com.google.android.apps.plus");
                Drawable icon = activity.getResources().getDrawable(iconDrawableId);
                icon.setBounds(0, 0, icon.getMinimumWidth(), icon.getMinimumHeight());
                title.setCompoundDrawables(icon, null, null, null);
            }

            if (position == 3) {
                FrameLayout layout = new FrameLayout(activity);
                layout.addView(view);

                FrameLayout counterLayout = new FrameLayout(activity);
                counterLayout.setLayoutParams(new FrameLayout.LayoutParams(dip2px(64), FrameLayout.LayoutParams.MATCH_PARENT));

                counter = new ImageView(activity);
                FrameLayout.LayoutParams counterParams = new FrameLayout.LayoutParams(dip2px(8), dip2px(8));
                counterParams.setMargins(0, dip2px(16), dip2px(27), 0);
                counterParams.gravity = Gravity.END | Gravity.CENTER | Gravity.TOP;
                counter.setLayoutParams(counterParams);

                ShapeDrawable shapeDrawable = new ShapeDrawable(new Shape() {
                    @Override
                    public void draw(Canvas canvas, Paint paint) {
                        paint.setColor(0xffdb4437);
                        paint.setStyle(Paint.Style.FILL);
                        canvas.drawCircle(dip2px(4), dip2px(4), dip2px(4), paint);
                        paint.setStrokeWidth(dip2px(1) / 3 * 2);
                        paint.setColor(Color.WHITE);
                        paint.setStyle(Paint.Style.STROKE);
                        canvas.drawCircle(dip2px(4), dip2px(4), dip2px(4), paint);
                    }
                });
                shapeDrawable.setIntrinsicWidth(dip2px(8));
                shapeDrawable.setIntrinsicHeight(dip2px(8));
                counter.setImageDrawable(shapeDrawable);
                counter.setVisibility(tabBarCounter.getVisibility());
                counterLayout.addView(counter);

                layout.addView(counterLayout);
                view = layout;
            }

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.onBackPressed();
                    if (position != 3)
                        tabButtons[position].performClick();
                    else
                        ((View) tabButtons[position].getParent()).performClick();
                }
            });
            views[position] = view;
            return view;
        }

        public int dip2px(float dipValue){
            final float scale = activity.getResources().getDisplayMetrics().density;
            return (int)(dipValue * scale + 0.5f);
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            /*activity.onBackPressed();
            if (position != 3)
                tabButtons[position].performClick();
            else
                ((View) tabButtons[position].getParent()).performClick();*/
        }
    }
}
