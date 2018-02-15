package li.lingfeng.ltweaks.xposed.system;

import android.os.Build;
import android.view.Menu;
import android.view.MenuItem;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.prefs.Prefs;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.Triple;
import li.lingfeng.ltweaks.utils.Utils;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/11/29.
 */
@XposedLoad(packages = {}, prefs = {},
        excludedPackages = PackageNames.ANDROID,
        loadAtActivityCreate = ClassNames.ACTIVITY)
public class XposedTextActions extends XposedBase {

    private static final String FLOATING_TOOLBAR = "com.android.internal.widget.FloatingToolbar";

    @Override
    protected void handleLoadPackage() throws Throwable {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        Prefs.instance().registerPreferenceChangeKey(R.string.key_text_actions_set);
        findAndHookMethod(FLOATING_TOOLBAR, "getVisibleAndEnabledMenuItems", Menu.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Set<String> savedItems = Prefs.instance().getStringSet(R.string.key_text_actions_set, null);
                if (savedItems == null || savedItems.size() == 0) {
                    return;
                }

                final Map<String, Triple<Integer, Boolean, String>> savedItemMap = new HashMap<>(savedItems.size());
                for (String savedItem : savedItems) {
                    String[] strs = Utils.splitReach(savedItem, ':', 5);
                    String name = strs[3];
                    String rename = strs[4];
                    int order = Integer.parseInt(strs[0]);
                    boolean block = Boolean.parseBoolean(strs[1]);
                    savedItemMap.put(name.toUpperCase(), new Triple(order, block, rename));
                }

                List<MenuItem> items = (List<MenuItem>) param.getResult();
                for (int i = items.size() - 1; i >= 0; --i) {
                    MenuItem item = items.get(i);
                    Triple<Integer, Boolean, String> triple = savedItemMap.get(item.getTitle().toString().toUpperCase());
                    if (triple != null && triple.second) {
                        Logger.d("Remove floating menu " + item.getTitle());
                        items.remove(i);
                    }
                }

                Logger.d("Sort floating menu.");
                Collections.sort(items, new Comparator<MenuItem>() {
                    @Override
                    public int compare(MenuItem i1, MenuItem i2) {
                        Triple<Integer, Boolean, String> triple = savedItemMap.get(i1.getTitle().toString().toUpperCase());
                        Integer order1 = triple == null ? null : triple.first;
                        triple = savedItemMap.get(i2.getTitle().toString().toUpperCase());
                        Integer order2 = triple == null ? null : triple.first;
                        if (order1 == null && order2 == null) {
                            return 0;
                        }
                        if (order1 == null) {
                            return 1;
                        }
                        if (order2 == null) {
                            return -1;
                        }
                        return order1 - order2;
                    }
                });

                for (MenuItem item : items) {
                    Triple<Integer, Boolean, String> triple = savedItemMap.get(item.getTitle().toString().toUpperCase());
                    if (triple != null && !StringUtils.isBlank(triple.third)) {
                        item.setTitle(triple.third);
                    }
                }
            }
        });
    }
}
