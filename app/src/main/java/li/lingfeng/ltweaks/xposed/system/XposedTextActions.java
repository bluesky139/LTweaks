package li.lingfeng.ltweaks.xposed.system;

import android.content.Intent;
import android.content.pm.ResolveInfo;
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
import de.robv.android.xposed.XposedHelpers;
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
    private static final String EDITOR = "android.widget.Editor";
    private static final String EDITOR_TEXT_ACTION_HANDLER = "android.widget.Editor$ProcessTextIntentActionsHandler";

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
                if (XposedHelpers.getAdditionalInstanceField(param.getResult(), "mIsSorted") != null) {
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
                    String title = item.getTitle().toString().toUpperCase();
                    title = StringUtils.strip(title, "\u200F\u200E ");
                    Triple<Integer, Boolean, String> triple = savedItemMap.get(title);
                    if (triple != null && triple.second) {
                        Logger.d("Remove floating menu " + item.getTitle());
                        items.remove(i);
                    }
                }

                Logger.d("Sort floating menu.");
                Collections.sort(items, new Comparator<MenuItem>() {
                    @Override
                    public int compare(MenuItem i1, MenuItem i2) {
                        String title1 = i1.getTitle().toString().toUpperCase();
                        title1 = StringUtils.strip(title1, "\u200F\u200E ");
                        Triple<Integer, Boolean, String> triple = savedItemMap.get(title1);
                        Integer order1 = triple == null ? null : triple.first;

                        String title2 = i2.getTitle().toString().toUpperCase();
                        title2 = StringUtils.strip(title2, "\u200F\u200E ");
                        triple = savedItemMap.get(title2);
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
                    String title = item.getTitle().toString().toUpperCase();
                    title = StringUtils.strip(title, "\u200F\u200E ");
                    Triple<Integer, Boolean, String> triple = savedItemMap.get(title);
                    if (triple != null && !StringUtils.isBlank(triple.third)) {
                        item.setTitle(triple.third);
                    }
                }

                // Remove title duplicated menu items after sorting and renaming.
                for (int i = items.size() - 1; i > 0; --i) {
                    String title = items.get(i).getTitle().toString().toUpperCase();
                    title = StringUtils.strip(title, "\u200F\u200E ");
                    for (int j = i - 1; j >= 0; --j) {
                        String title2 = items.get(j).getTitle().toString().toUpperCase();
                        title2 = StringUtils.strip(title2, "\u200F\u200E ");
                        if (title.equals(title2)) {
                            Logger.d("Remove duplicated " + title);
                            items.remove(i);
                            break;
                        }
                    }
                }

                XposedHelpers.setAdditionalInstanceField(items, "mIsSorted", true);
            }
        });

        // https://github.com/aosp-mirror/platform_frameworks_base/blob/oreo-release/core/java/android/widget/Editor.java#L6354
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            findAndHookMethod(EDITOR_TEXT_ACTION_HANDLER, "onInitializeMenu", Menu.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Menu menu = (Menu) param.args[0];
                    XposedHelpers.callMethod(param.thisObject, "loadSupportedActivities");
                    List<ResolveInfo> supportedActivities = (List<ResolveInfo>) XposedHelpers.getObjectField(param.thisObject, "mSupportedActivities");
                    int start = XposedHelpers.getStaticIntField(findClass(EDITOR), "MENU_ITEM_ORDER_PROCESS_TEXT_INTENT_ACTIONS_START");
                    for (int i = 0; i < supportedActivities.size(); ++i) {
                        final ResolveInfo resolveInfo = supportedActivities.get(i);
                        Logger.d("Text action supported activity " + resolveInfo.activityInfo.name);
                        menu.add(Menu.NONE, Menu.NONE,
                                start + i,
                                (CharSequence) XposedHelpers.callMethod(param.thisObject, "getLabel", resolveInfo))
                                .setIntent((Intent) XposedHelpers.callMethod(param.thisObject, "createProcessTextIntentForResolveInfo", resolveInfo))
                                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                    }
                    param.setResult(null);
                }
            });
        }
    }
}
