package li.lingfeng.ltweaks.utils;

import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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

    public static ViewGroup findViewGroupByName(final ViewGroup rootView, String name) {
        Queue<ViewGroup> views = new LinkedList<>();
        views.add(rootView);
        while (views.size() > 0) {
            ViewGroup view = views.poll();
            //Logger.d("findViewGroupByName " + view);
            if (view.getId() > 0) {
                String name_ = ContextUtils.getResNameById(view.getId());
                if (name.equals(name_)) {
                    return view;
                }
            }

            for (int i = 0; i < view.getChildCount(); ++i) {
                View child = view.getChildAt(i);
                if (child instanceof ViewGroup) {
                    views.add((ViewGroup) child);
                }
            }
        }
        return null;
    }

    public static List<View> findAllViewByName(ViewGroup rootView, String name) {
        Queue<View> views = new LinkedList<>();
        for (int i = 0; i < rootView.getChildCount(); ++i) {
            View child = rootView.getChildAt(i);
            views.add(child);
        }

        List<View> results = new ArrayList<>();
        while (views.size() > 0) {
            View view = views.poll();
            //Logger.d("findAllViewByName " + view);
            if (view.getId() > 0) {
                String name_ = ContextUtils.getResNameById(view.getId());
                if (name.equals(name_)) {
                    results.add(view);
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
}
