package li.lingfeng.ltweaks.xposed.system;

import android.app.Application;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;

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
import li.lingfeng.ltweaks.prefs.SharedPreferences;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.Utils;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/11/29.
 */
@XposedLoad(packages = {}, prefs = R.string.key_text_actions_enable,
        excludedPackages = PackageNames.ANDROID/*,
        loadAtActivityCreate = ClassNames.ACTIVITY*/)
public class XposedTextActions extends XposedBase {

    private static final String FLOATING_TOOLBAR = "com.android.internal.widget.FloatingToolbar";
    private Map<String, Pair<Integer, Boolean>> mSavedItems;

    @Override
    protected void handleLoadPackage() throws Throwable {
        /*Prefs.instance().registerPreferenceChangeKey(R.string.key_text_actions_set, new SharedPreferences.OnPreferenceChangeListener() {
            @Override
            public void onChanged(String key, Object value) {
                mSavedItems = null;
            }
        });*/

        findAndHookMethod(FLOATING_TOOLBAR, "getVisibleAndEnabledMenuItems", Menu.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Set<String> savedItems = Prefs.instance().getStringSet(R.string.key_text_actions_set, null);
                if (savedItems == null || savedItems.size() == 0) {
                    return;
                }

                if (mSavedItems == null) {
                    mSavedItems = new HashMap<>(savedItems.size());
                    for (String item : savedItems) {
                        String[] strs = Utils.splitMax(item, ':', 4);
                        String name = strs[3];
                        int order = Integer.parseInt(strs[0]);
                        boolean block = Boolean.parseBoolean(strs[1]);
                        mSavedItems.put(name, Pair.create(order, block));
                    }
                }

                List<MenuItem> items = (List<MenuItem>) param.getResult();
                for (int i = items.size() - 1; i >= 0; --i) {
                    MenuItem item = items.get(i);
                    Pair<Integer, Boolean> pair = mSavedItems.get(item.getTitle());
                    if (pair != null && pair.second) {
                        Logger.d("Remove floating menu " + item.getTitle());
                        items.remove(i);
                    }
                }

                Logger.d("Sort floating menu.");
                Collections.sort(items, new Comparator<MenuItem>() {
                    @Override
                    public int compare(MenuItem i1, MenuItem i2) {
                        Pair<Integer, Boolean> pair = mSavedItems.get(i1.getTitle());
                        Integer order1 = pair == null ? null : pair.first;
                        pair = mSavedItems.get(i2.getTitle());
                        Integer order2 = pair == null ? null : pair.first;
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
            }
        });
    }
}
