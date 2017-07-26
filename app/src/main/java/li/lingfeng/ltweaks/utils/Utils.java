package li.lingfeng.ltweaks.utils;

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
