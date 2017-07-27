package li.lingfeng.ltweaks.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
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
            public boolean onAddResult(View view) {
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
            public boolean onAddResult(View view) {
                if (view.getId() > 0) {
                    String name_ = ContextUtils.getResNameById(view.getId());
                    return name.equals(name_);
                }
                return false;
            }
        });
    }

    public static <T extends View> List<T> findAllViewByType(ViewGroup rootView, final Class<? extends View> type) {
        return traverseViews(rootView, false, new ViewTraverseCallback() {
            @Override
            public boolean onAddResult(View view) {
                return type.isAssignableFrom(view.getClass());
            }
        });
    }

    public static <T extends View> T findViewByType(ViewGroup rootView, final Class<? extends View> type) {
        List<View> views = traverseViews(rootView, true, new ViewTraverseCallback() {
            @Override
            public boolean onAddResult(View view) {
                return type.isAssignableFrom(view.getClass());
            }
        });
        if (views.size() > 0) {
            return (T) views.get(0);
        }
        return null;
    }

    private static <T extends View> List<T> traverseViews(ViewGroup rootView, boolean onlyOne, ViewTraverseCallback callback) {
        Queue<View> views = new LinkedList<>();
        for (int i = 0; i < rootView.getChildCount(); ++i) {
            View child = rootView.getChildAt(i);
            views.add(child);
        }

        List<T> results = new ArrayList<>();
        while (views.size() > 0) {
            View view = views.poll();
            //Logger.v("traverseViews " + view);
            if (callback.onAddResult(view)) {
                results.add((T) view);
                if (onlyOne) {
                    return results;
                }
            }

            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); ++i) {
                    View child = viewGroup.getChildAt(i);
                    views.add(child);
                }
            }
        }
        return results;
    }

    private interface ViewTraverseCallback {
        boolean onAddResult(View view);
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
        FrameLayout allView = new FrameLayout(activity);
        while (rootView.getChildCount() > 0) {
            View view = rootView.getChildAt(0);
            rootView.removeView(view);
            allView.addView(view);
        }
        return allView;
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
}
