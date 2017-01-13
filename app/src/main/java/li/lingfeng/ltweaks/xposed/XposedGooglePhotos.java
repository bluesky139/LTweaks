package li.lingfeng.ltweaks.xposed;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;

import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

/**
 * Created by smallville on 2017/1/4.
 */
@XposedLoad(packages = "com.google.android.apps.photos", prefs = R.string.key_google_photos_remove_bottom_bar)
public class XposedGooglePhotos implements IXposedHookLoadPackage {

    Activity activity;
    View rootView;

    View tabBar;
    Button tabAssistant;
    Button tabPhotos;
    Button tabAlbums;
    Button[] tabButtons = new Button[3];

    LinearLayout drawerFragment;
    ListView navList;
    ListView barList;
    BarListAdapter barListAdapter;

    FrameLayout navLayout;
    ScrollView scrollView;
    LinearLayout scrollLayout;

    boolean done = false;
    boolean doneWithPageTransformer = false;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        findAndHookMethod("com.google.android.apps.photos.app.PhotosApplication", lpparam.classLoader, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                Application app = (Application) param.thisObject;
                app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                        if (!activity.getClass().getName().equals("com.google.android.apps.photos.home.HomeActivity"))
                            return;
                        XposedGooglePhotos.this.activity = activity;
                        rootView = activity.findViewById(android.R.id.content);
                        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                if (done)
                                    return;
                                //Log.w(TAG, "layout changed.");
                                traverseViewChilds(rootView, 0);
                                if (!done && isReady())
                                    createBarList();
                            }
                        });

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            XposedGooglePhotos.this.activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                        }
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
                        if (!activity.getClass().getName().equals("com.google.android.apps.photos.home.HomeActivity"))
                            return;
                        XposedGooglePhotos.this.activity = null;
                        rootView = null;
                        tabBar = null;
                        tabAssistant = null;
                        tabPhotos = null;
                        tabAlbums = null;
                        tabButtons = new Button[3];
                        drawerFragment = null;
                        barList = null;
                        barListAdapter = null;
                        done = false;
                    }
                });
            }
        });

        /*findAndHookConstructor("com.google.android.apps.photos.home.LockableViewPager", lpparam.classLoader, Context.class, AttributeSet.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (doneWithPageTransformer)
                    return;

                Class pageTransformer = null;
                Class viewPager = activity.getClassLoader().loadClass("android.support.v4.view.ViewPager");
                Field[] fields = viewPager.getDeclaredFields();
                for (Field field : fields) {
                    int flag = field.getModifiers();
                    if (!Modifier.isPrivate(flag) || Modifier.isFinal(flag) || Modifier.isStatic(flag))
                        continue;
                    flag = field.getType().getModifiers();
                    if (!Modifier.isInterface(flag) || !Modifier.isAbstract(flag) || field.getType().getDeclaredMethods().length != 1)
                        continue;
                    Class[] params = field.getType().getDeclaredMethods()[0].getParameterTypes();
                    if (params.length != 2 || params[0] != View.class || params[1] != float.class)
                        continue;
                    pageTransformer = field.getType();
                    Logger.i("Got PageTransformer " + field.getName() + " " + field.getType().getName());
                    break;
                }
                if (pageTransformer == null)
                    return;

                Method setPageTransformer = null;
                Method[] methods = viewPager.getDeclaredMethods();
                for (Method method : methods) {
                    int flag = method.getModifiers();
                    if (!Modifier.isPublic(flag) || !Modifier.isFinal(flag) || method.getParameterTypes().length != 2)
                        continue;
                    Class[] params = method.getParameterTypes();
                    if (params[0] != boolean.class || params[1] != pageTransformer)
                        continue;
                    setPageTransformer = method;
                    Logger.i("Got setPageTransformer");
                    break;
                }
                if (setPageTransformer == null)
                    return;

                findAndHookMethod("android.support.v4.view.ViewPager", lpparam.classLoader, setPageTransformer.getName(), boolean.class, pageTransformer, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);
                        if (param.thisObject.getClass().getName().equals("com.google.android.apps.photos.home.LockableViewPager")) {
                            param.args[0] = false;
                            param.args[1] = null;
                            Logger.i("Set to default page transformer.");
                        }
                    }
                });
                doneWithPageTransformer = true;
            }
        });

        findAndHookMethod("com.google.android.apps.photos.home.LockableViewPager", lpparam.classLoader, "onInterceptTouchEvent", MotionEvent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Field[] fields = param.thisObject.getClass().getDeclaredFields();
                for (Field field : fields) {
                    if (field.getGenericType() == Boolean.TYPE && field.getBoolean(param.thisObject)) {
                        field.setAccessible(true);
                        field.setBoolean(param.thisObject, false);
                        Logger.i("Modified boolean to false in LockableViewPager.");
                        break;
                    }
                }
            }
        });*/
    }

    boolean isReady() {
        if (activity != null && tabBar != null && tabAssistant != null && tabPhotos != null && tabAlbums != null && drawerFragment != null
                && navList != null)
            return true;
        return false;
    }

    void traverseViewChilds(View view, int depth) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); ++i) {
                view = viewGroup.getChildAt(i);
                //Log.d(TAG, "child view" + depth + " " + view + " id " + view.getId());
                if (tabBar == null && view.getId() > 0) {
                    if (activity.getResources().getResourceEntryName(view.getId()).equals("tab_bar")) {
                        Logger.i("got tab_bar.");
                        tabBar = view;
                        ViewGroup.LayoutParams tabBarParams = tabBar.getLayoutParams();
                        tabBarParams.height = 0;
                        tabBar.setLayoutParams(tabBarParams);
                        tabBar.setVisibility(View.INVISIBLE);
                    }
                }
                if (tabAssistant == null && view.getId() > 0) {
                    if (activity.getResources().getResourceEntryName(view.getId()).equals("tab_assistant")) {
                        Logger.i("got tab_assistant.");
                        tabAssistant = (Button) view;
                        tabButtons[0] = tabAssistant;
                    }
                }
                if (tabPhotos == null && view.getId() > 0) {
                    if (activity.getResources().getResourceEntryName(view.getId()).equals("tab_photos")) {
                        Logger.i("got tab_photos.");
                        tabPhotos = (Button) view;
                        tabButtons[1] = tabPhotos;
                    }
                }
                if (tabAlbums == null && view.getId() > 0) {
                    if (activity.getResources().getResourceEntryName(view.getId()).equals("tab_albums")) {
                        Logger.i("got tab_albums.");
                        tabAlbums = (Button) view;
                        tabButtons[2] = tabAlbums;
                    }
                }
                if (drawerFragment == null && view.getId() > 0) {
                    if (activity.getResources().getResourceEntryName(view.getId()).equals("navigation_list")) {
                        Logger.i("got navigation_list.");
                        drawerFragment = (LinearLayout) view.getParent().getParent();
                        navList = (ListView) view;
                        if (isReady())
                            createBarList();
                    }
                }
                traverseViewChilds(view, depth + 1);
            }
        }
    }

    void createBarList() {
        ListAdapter adapter = navList.getAdapter();
        if (adapter != null && adapter.getCount() > 3) {
            int titleId = activity.getResources().getIdentifier("title", "id", "com.google.android.apps.photos");
            View view = adapter.getView(0, null, null).findViewById(titleId);
            if (view != null) {
                TextView title = (TextView) view;
                if (title.getText().toString().equals(tabAssistant.getText().toString())) {
                    Logger.i("Drawer menu has buttons of bottom bar, skip create.");
                    done = true;
                    return;
                }
            }
        }

        barList = new ListView(XposedGooglePhotos.this.activity);
        barListAdapter = new BarListAdapter();
        View one = barListAdapter.getView(0, null, barList);
        one.measure(0, 0);
        int height = one.getMeasuredHeight();

        barList.setLayoutParams(new ListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height * barListAdapter.getCount()));
        barList.setHeaderDividersEnabled(false);
        barList.setFooterDividersEnabled(false);
        barList.setDividerHeight(0);
        barList.setAdapter(barListAdapter);
        barList.setOnItemClickListener(barListAdapter);

        ((ViewGroup) navList.getParent()).removeView(navList);
        navLayout = new FrameLayout(XposedGooglePhotos.this.activity);
        navLayout.addView(navList, ViewGroup.LayoutParams.MATCH_PARENT, height * navList.getAdapter().getCount());

        scrollLayout = new LinearLayout(XposedGooglePhotos.this.activity);
        scrollLayout.setOrientation(LinearLayout.VERTICAL);
        scrollLayout.addView(barList);
        scrollLayout.addView(navLayout);
        scrollView = new ScrollView(XposedGooglePhotos.this.activity);
        scrollView.addView(scrollLayout);
        drawerFragment.addView(scrollView, 1);

        done = true;
    }

    class BarListAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {

        View[] views = new View[3];

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

            int layoutId = activity.getResources().getIdentifier("photos_drawermenu_navigation_item", "layout", "com.google.android.apps.photos");
            View view = LayoutInflater.from(activity).inflate(layoutId, barList, false);
            int titleId = activity.getResources().getIdentifier("title", "id", "com.google.android.apps.photos");
            TextView title = (TextView) view.findViewById(titleId);
            title.setText(tabButtons[position].getText());
            int iconId = activity.getResources().getIdentifier("icon", "id", "com.google.android.apps.photos");
            ImageView icon = (ImageView) view.findViewById(iconId);
            String strDrwable = "albums_drawable";
            if (getItem(position) == tabAssistant) {
                strDrwable = "photos_drawermenu_navigation_ic_drawer_assistant";
            } else if (getItem(position) == tabPhotos) {
                strDrwable = "photos_drawable";
            }
            int iconDrawableId = activity.getResources().getIdentifier(strDrwable, "drawable", "com.google.android.apps.photos");
            if (iconDrawableId != 0) {
                icon.setImageDrawable(activity.getResources().getDrawable(iconDrawableId));
            }

            views[position] = view;
            return view;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            activity.onBackPressed();
            tabButtons[position].performClick();
        }
    }
}
