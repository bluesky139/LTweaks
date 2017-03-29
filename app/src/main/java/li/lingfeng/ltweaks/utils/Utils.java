package li.lingfeng.ltweaks.utils;

import java.util.Collection;

/**
 * Created by smallville on 2017/3/29.
 */

public class Utils {

    public static String stringJoin(Collection<String> collection) {
        StringBuilder builder = new StringBuilder();
        for (String s : collection) {
            if (collection.size() > 0)
                builder.append('\n');
            builder.append(s);
        }
        return builder.toString();
    }
}
