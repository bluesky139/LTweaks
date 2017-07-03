package li.lingfeng.ltweaks.utils;

import java.util.Collection;

/**
 * Created by smallville on 2017/3/29.
 */

public class Utils {

    public static String[] splitByLastChar(String str, char ch) {
        int pos = str.lastIndexOf(ch);
        return new String[] { str.substring(0, pos), str.substring(pos + 1) };
    }
}
