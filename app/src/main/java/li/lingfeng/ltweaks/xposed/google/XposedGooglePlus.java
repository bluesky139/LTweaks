package li.lingfeng.ltweaks.xposed.google;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.prefs.Prefs;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.ViewUtils;
import li.lingfeng.ltweaks.xposed.XposedBase;

import static li.lingfeng.ltweaks.utils.ContextUtils.dp2px;

/**
 * Created by smallville on 2017/1/4.
 */
@XposedLoad(packages = PackageNames.GOOGLE_PLUS, prefs = R.string.key_google_plus_remove_bottom_bar)
public class XposedGooglePlus extends XposedBase {

    private static final String sActivityName = "com.google.android.apps.plus.home.TikTokHomeActivity";

    private Activity activity;
    private View rootView;

    private View tabBar;
    private int tabBarSpacerId = 0;
    private TextView[] tabButtons = new TextView[4];
    private View tabBarCounter;

    private View accountView;
    private ImageView counter;

    private boolean done = false;

    private View newPostsButton;
    private MenuItem refreshMenu;

    @Override
    public void handleLoadPackage() throws Throwable {
        findAndHookActivity(sActivityName, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                activity = (Activity) param.thisObject;
                tabBarSpacerId = ContextUtils.getIdId("bottom_navigation_spacer");
                if (tabBarSpacerId == 0)
                    return;

                rootView = activity.findViewById(android.R.id.content);
                rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        //Logger.w("layout changed.");
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

                        if (newPostsButton != null && refreshMenu != null) {
                            Logger.i("newPostsButton's visibility " + newPostsButton.getVisibility());
                            if (newPostsButton.getVisibility() == View.VISIBLE) {
                                newPostsButton.setVisibility(View.INVISIBLE);
                                refreshMenu.setVisible(true);
                            } else if (newPostsButton.getVisibility() == View.GONE) {
                                refreshMenu.setVisible(false);
                            }
                        }
                    }
                });
            }
        });

        findAndHookActivity(sActivityName, "onCreateOptionsMenu", Menu.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!Prefs.instance().getBoolean(R.string.key_google_plus_top_right_refresh, false)) {
                    return;
                }
                Logger.i("Add menu \"Refresh\"");
                Menu menu = (Menu) param.args[0];
                refreshMenu = menu.add(Menu.NONE, Menu.NONE, 1000, "Refresh");
                refreshMenu.setIcon(ContextUtils.getResId("quantum_ic_refresh_grey600_24", "drawable"));
                refreshMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
        });

        findAndHookActivity(sActivityName, "onOptionsItemSelected", MenuItem.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                MenuItem item = (MenuItem) param.args[0];
                if (item != refreshMenu) {
                    return;
                }

                Logger.i("Menu \"Refresh\" is clicked.");
                if (newPostsButton != null) {
                    newPostsButton.performClick();
                }
            }
        });

        findAndHookActivity(sActivityName, "onDestroy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                activity = null;
                rootView = null;
                tabBar = null;
                tabBarSpacerId = 0;
                Arrays.fill(tabButtons, null);
                tabBarCounter = null;
                newPostsButton = null;
                refreshMenu = null;
                accountView = null;
                counter = null;
                done = false;
            }
        });
    }

    private boolean isReady() {
        if (activity != null && tabBar != null &&  accountView != null) {
            for (View button : tabButtons) {
                if (button == null)
                    return false;
            }
            return true;
        }
        return false;
    }

    private void traverseViewChilds(View view, int depth) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); ++i) {
                view = viewGroup.getChildAt(i);
                //Logger.v("child view" + depth + " " + view + " id " + view.getId());
                if (tabBar == null) {
                    if (getResNameById(view.getId()).equals("navigation_bottom_bar")) {
                        Logger.i("got navigation_bottom_bar.");
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
                if (view.getId() > 0) {
                    if (getResNameById(view.getId()).equals("new_posts_button")) {
                        Logger.i("got new_posts_button.");
                        newPostsButton = view;
                    }
                }
                if (accountView == null && view.getId() > 0) {
                    if (getResNameById(view.getId()).equals("account_switcher_view")) {
                        Logger.i("got account_switcher_view.");
                        accountView = view;
                    }
                }
                traverseViewChilds(view, depth + 1);
            }
        }
    }

    private String getResNameById(int id) {
        if (id < 0x7F000000)
            return "";
        try {
            return activity.getResources().getResourceEntryName(id);
        } catch (Exception e) {
            return "";
        }
    }

    private void createBarList() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                final ViewGroup drawerListContainer = (ViewGroup) accountView.getParent();
                View view = ViewUtils.findViewByName(drawerListContainer, "nav_menu_item_text");
                ViewGroup viewGroup = (ViewGroup) view.getParent();
                int height = viewGroup.getHeight();
                Logger.d("One height " + height);

                LinearLayout listLayout = new LinearLayout(activity);
                listLayout.setOrientation(LinearLayout.VERTICAL);
                for (int i = 0; i < tabButtons.length; ++i) {
                    View itemView = createListItem(i);
                    listLayout.addView(itemView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
                }
                drawerListContainer.addView(listLayout, drawerListContainer.indexOfChild(accountView) + 1);
            }
        }, 1000);
        done = true;
    }

    public View createListItem(final int position) {
        int layoutId = activity.getResources().getIdentifier("nav_item", "layout", "com.google.android.apps.plus");
        View view = LayoutInflater.from(activity).inflate(layoutId, null);
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                activity.getResources().getDimensionPixelSize(ContextUtils.getDimenId("nav_item_height"))));
        int titleId = activity.getResources().getIdentifier("nav_menu_item_text", "id", "com.google.android.apps.plus");
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

        if (position == 3) {
            FrameLayout layout = new FrameLayout(activity);
            layout.addView(view);

            FrameLayout counterLayout = new FrameLayout(activity);
            counterLayout.setLayoutParams(new FrameLayout.LayoutParams(dp2px(64), FrameLayout.LayoutParams.MATCH_PARENT));

            counter = new ImageView(activity);
            FrameLayout.LayoutParams counterParams = new FrameLayout.LayoutParams(dp2px(8), dp2px(8));
            counterParams.setMargins(0, dp2px(16), dp2px(27), 0);
            counterParams.gravity = Gravity.END | Gravity.CENTER | Gravity.TOP;
            counter.setLayoutParams(counterParams);

            ShapeDrawable shapeDrawable = new ShapeDrawable(new Shape() {
                @Override
                public void draw(Canvas canvas, Paint paint) {
                    paint.setColor(0xffdb4437);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(dp2px(4), dp2px(4), dp2px(4), paint);
                    paint.setStrokeWidth(dp2px(1) / 3 * 2);
                    paint.setColor(Color.WHITE);
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawCircle(dp2px(4), dp2px(4), dp2px(4), paint);
                }
            });
            shapeDrawable.setIntrinsicWidth(dp2px(8));
            shapeDrawable.setIntrinsicHeight(dp2px(8));
            counter.setImageDrawable(shapeDrawable);
            counter.setVisibility(tabBarCounter.getVisibility());
            counterLayout.addView(counter);

            layout.addView(counterLayout);
            view = layout;
        }
        return view;
    }
}
