package li.lingfeng.ltweaks.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.WebView;
import android.widget.FrameLayout;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import li.lingfeng.ltweaks.R;

/**
 * Created by smallville on 2017/2/9.
 */

public class ViewUtils {

    public static List<View> findAllViewByName(ViewGroup rootView, String containerName, String name) {
        if (containerName != null)
            rootView = findViewGroupByName(rootView, containerName);
        if (rootView == null)
            return new ArrayList<>();
        return findAllViewByName(rootView, name);
    }

    public static ViewGroup findViewGroupByName(final ViewGroup rootView, final String name) {
        List<View> views = traverseViews(rootView, true, new ViewTraverseCallback() {
            @Override
            public boolean onAddResult(View view, int deep) {
                if (view instanceof ViewGroup && view.getId() > 0) {
                    String name_ = ContextUtils.getResNameById(view.getId());
                    return name.equals(name_);
                }
                return false;
            }
        });
        if (views.size() > 0) {
            return (ViewGroup) views.get(0);
        }
        return null;
    }

    public static List<View> findAllViewByName(ViewGroup rootView, final String name) {
        return traverseViews(rootView, false, new ViewTraverseCallback() {
            @Override
            public boolean onAddResult(View view, int deep) {
                if (view.getId() > 0) {
                    String name_ = ContextUtils.getResNameById(view.getId());
                    return name.equals(name_);
                }
                return false;
            }
        });
    }

    public static <T extends View> List<T> findAllViewByType(ViewGroup rootView, final Class<T> type) {
        return traverseViews(rootView, false, new ViewTraverseCallback() {
            @Override
            public boolean onAddResult(View view, int deep) {
                return type.isAssignableFrom(view.getClass());
            }
        });
    }

    public static <T extends View> List<T> findAllViewByTypeInSameHierarchy(final ViewGroup rootView,
                                                                            final Class<T> type,
                                                                            final int minCount) {
        final List<T> results = new ArrayList<>();
        traverseViews(rootView, new ViewTraverseCallback2() {

            private int mDeep = 0;
            private ViewParent mParent = rootView;

            @Override
            public boolean onView(View view, int deep) {
                if (!type.isAssignableFrom(view.getClass())) {
                    return false;
                }

                if (mDeep != deep || view.getParent() != mParent) {
                    if (results.size() >= minCount) {
                        return true;
                    }
                    mDeep = deep;
                    mParent = view.getParent();
                    results.clear();
                }
                results.add((T) view);
                return false;
            }
        });
        return results.size() >= minCount ? results : new ArrayList<T>();
    }

    public static <T extends View> T findViewByType(ViewGroup rootView, final Class<? extends View> type) {
        List<View> views = traverseViews(rootView, true, new ViewTraverseCallback() {
            @Override
            public boolean onAddResult(View view, int deep) {
                return type.isAssignableFrom(view.getClass());
            }
        });
        if (views.size() > 0) {
            return (T) views.get(0);
        }
        return null;
    }

    public static void printChilds(ViewGroup rootView) {
        traverseViews(rootView, false, new ViewTraverseCallback() {
            @Override
            public boolean onAddResult(View view, int deep) {
                Logger.v(" child[" + deep + "] " + view);
                return false;
            }
        });
    }

    public static <T extends View> List<T> traverseViews(ViewGroup rootView, final boolean onlyOne, final ViewTraverseCallback callback) {
        final List<T> results = new ArrayList<>();
        traverseViews(rootView, new ViewTraverseCallback2() {
            @Override
            public boolean onView(View view, int deep) {
                if (callback.onAddResult(view, deep)) {
                    results.add((T) view);
                    if (onlyOne) {
                        return true;
                    }
                }
                return false;
            }
        });
        return results;
    }

    public static void traverseViews(ViewGroup rootView, ViewTraverseCallback2 callback) {
        Queue<Pair<View, Integer>> views = new LinkedList<>();
        for (int i = 0; i < rootView.getChildCount(); ++i) {
            View child = rootView.getChildAt(i);
            views.add(Pair.create(child, 0));
        }

        while (views.size() > 0) {
            Pair<View, Integer> pair = views.poll();
            View view = pair.first;
            int deep = pair.second;
            if (callback.onView(view, deep)) {
                return;
            }

            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); ++i) {
                    View child = viewGroup.getChildAt(i);
                    views.add(Pair.create(child, deep + 1));
                }
            }
        }
    }

    public interface ViewTraverseCallback {
        boolean onAddResult(View view, int deep); // Return true to abort.
    }

    public interface ViewTraverseCallback2 {
        boolean onView(View view, int deep); // Return true to abort.
    }

    public static Fragment findFragmentByPosition(FragmentManager fragmentManager, ViewPager viewPager, int position) {
        try {
            Method method = FragmentPagerAdapter.class.getDeclaredMethod("makeFragmentName", int.class, long.class);
            method.setAccessible(true);
            String tag = (String) method.invoke(viewPager.getAdapter(), viewPager.getId(), position);
            return fragmentManager.findFragmentByTag(tag);
        } catch (Exception e) {
            Logger.e("findFragmentByPosition error, " + e);
            return null;
        }
    }

    // Views will be detached from activity.
    public static FrameLayout rootChildsIntoOneLayout(Activity activity) {
        ViewGroup rootView = (ViewGroup) activity.findViewById(android.R.id.content);
        return viewChildsIntoOneLayout(activity, rootView);
    }

    // Views will be detached from activity.
    public static FrameLayout viewChildsIntoOneLayout(Activity activity, ViewGroup rootView) {
        FrameLayout allView = new FrameLayout(activity);
        allView.setContentDescription("allView");
        while (rootView.getChildCount() > 0) {
            View view = rootView.getChildAt(0);
            rootView.removeView(view);
            allView.addView(view);
        }
        return allView;
    }

    public static View nextView(View view) {
        ViewGroup parent = (ViewGroup) view.getParent();
        return parent.getChildAt(parent.indexOfChild(view) + 1);
    }

    public static void removeView(View view) {
        ViewGroup parent = (ViewGroup) view.getParent();
        parent.removeView(view);
    }

    public static void showDialog(Context context, String message) {
        new AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton(R.string.app_ok, null)
                .show();
    }

    public static void executeJs(WebView webView, String js) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(js, null);
        } else {
            webView.loadUrl("javascript:" + js);
        }
    }

    public static int getWindowHeight(Activity activity) {
        return activity.getWindowManager().getDefaultDisplay().getHeight();
    }

    public static int getWindowHeightWithNavigator(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        try {
            Class c = Class.forName("android.view.Display");
            Method method = c.getMethod("getRealMetrics", DisplayMetrics.class);
            method.setAccessible(true);
            method.invoke(display, dm);
            return dm.heightPixels;
        } catch (Throwable e) {
            Logger.e("Can't getWindowHeightWithNavigator, " + e);
            return getWindowHeight(activity);
        }
    }
}
