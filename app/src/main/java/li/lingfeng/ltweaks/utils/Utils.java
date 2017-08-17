package li.lingfeng.ltweaks.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Collection;

/**
 * Created by smallville on 2017/3/29.
 */

public class Utils {

    public static String[] splitByLastChar(String str, char ch) {
        int pos = str.lastIndexOf(ch);
        return new String[] { str.substring(0, pos), str.substring(pos + 1) };
    }

    public static boolean pairContains(Pair[] pairs, Object o, boolean isFirst) {
        for (Pair pair : pairs) {
            if (isFirst ? pair.first.equals(o) : pair.second.equals(o)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isUrl(String str) {
        return Patterns.WEB_URL.matcher(str).matches()
                && (str.toLowerCase().startsWith("http://")
                || str.toLowerCase().startsWith("https://"));
    }

    public static MenuItem findMenuItemByTitle(Menu menu, final String title) {
        return findMenuItemBy(menu, new FindMenuItemCallback() {
            @Override
            public boolean onMenuItem(MenuItem item) {
                return title.equals(item.getTitle());
            }
        });
    }

    public static MenuItem findMenuItemById(Menu menu, final int id) {
        return findMenuItemBy(menu, new FindMenuItemCallback() {
            @Override
            public boolean onMenuItem(MenuItem item) {
                return id == item.getItemId();
            }
        });
    }

    private static MenuItem findMenuItemBy(Menu menu, FindMenuItemCallback callback) {
        for (int i = 0; i < menu.size(); ++i) {
            MenuItem item = menu.getItem(i);
            if (callback.onMenuItem(item)) {
                return item;
            }
        }
        return null;
    }

    private interface FindMenuItemCallback {
        boolean onMenuItem(MenuItem item);
    }
}
