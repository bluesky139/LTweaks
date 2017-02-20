package li.lingfeng.ltweaks.utils;

import android.support.annotation.IntDef;
import android.util.Pair;
import android.util.Patterns;
import android.util.SparseArray;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by smallville on 2017/2/19.
 */

public class ShoppingUtils {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STORE_JD, STORE_SUNING})
    public @interface Store {}
    public static final int STORE_JD     = 3;
    public static final int STORE_SUNING = 25;
    public static final int _STORE_COUNT = 2;

    private static SparseArray<Pattern[]> sItemIdPatterns = new SparseArray<Pattern[]>(_STORE_COUNT) {{
        put(STORE_JD, new Pattern[] {
                Pattern.compile("https?://re\\.jd\\.com/cps/item/(\\d+)\\.html"),
                Pattern.compile("https?://item\\.jd\\.com/(\\d+)\\.html"),
                Pattern.compile("https?://(item\\.)?m\\.jd\\.com/product/(\\d+)\\.html"),
                //Pattern.compile("https?://.*\\.jd\\.com/.*sku%3D(\\d+)"),
                //Pattern.compile("https?://.*\\.jd\\.com/.*sku=(\\d+)"),
                //Pattern.compile("https?://.*\\.jd\\.com/.*/product/(\\d+)"),
                //Pattern.compile("https?://.*\\.jd\\.com/.*%2Fproduct%2F(\\d+)"),
                Pattern.compile("https?(://|%3A%2F%2F).*\\.jd\\.com(/.*)?(/|%2F|\\?|%3F)(product|sku)(/|%2F|=|%3D)(\\d+)")
        });
        put(STORE_SUNING, new Pattern[] {
                Pattern.compile("https?://m\\.suning\\.com/product/\\d+/(\\d+)\\.html"),
                Pattern.compile("https?://product\\.suning\\.com/\\d+/(\\d+).html"),
                Pattern.compile("https?%3A%2F%2Fm\\.suning\\.com%2Fproduct%2F\\d+%2F0*(\\d+).html")
        });
    }};

    public static Pair<String, Integer> findItemId(String text) {
        for (int i = 0; i < sItemIdPatterns.size(); ++i) {
            @Store int store = sItemIdPatterns.keyAt(i);
            String itemId = findItemIdByStore(text, store);
            if (itemId != null)
                return new Pair<>(itemId, store);
        }
        return null;
    }

    public static String findItemIdByStore(String text, @Store int store) {
        for (Pattern pattern : sItemIdPatterns.get(store)) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String itemId = matcher.group(matcher.groupCount());
                Logger.i("Got item id " + itemId + " from " + text);
                return itemId;
            }
        }
        return null;
    }
}
