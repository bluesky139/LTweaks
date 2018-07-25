package li.lingfeng.ltweaks.xposed.system;

import android.annotation.TargetApi;
import android.graphics.Rect;
import android.os.Build;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/7/20.
 */
@XposedLoad(packages = {}, prefs = R.string.key_selection_action_mode_original)
public class XposedOriginalSelectionActionMode extends XposedBase {
    @Override
    protected void handleLoadPackage() throws Throwable {
        if (lpparam.packageName.equals(PackageNames.ANDROID)
                || lpparam.packageName.equals(PackageNames.ANDROID_SYSTEM_UI)) {
            return;
        }

        findAndHookMethod(TextView.class, "setCustomSelectionActionModeCallback", ActionMode.Callback.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                ActionMode.Callback original = (ActionMode.Callback) param.args[0];
                Logger.i("setCustomSelectionActionModeCallback middle callback for " + original);
                param.args[0] = new MiddleCallback(original);
                XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                param.setResult(null);
            }
        });

        findAndHookMethod(TextView.class, "canProcessText", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("canProcessText return true");
                param.setResult(true);
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.M)
    private class MiddleCallback extends ActionMode.Callback2 {

        private ActionMode.Callback mOriginal;
        private Map<CharSequence, MenuItem> mOriginalItems;

        MiddleCallback(ActionMode.Callback original) {
            mOriginal = original;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            mOriginalItems = new LinkedHashMap<>();
            for (int i = 0; i < menu.size(); ++i) {
                MenuItem item = menu.getItem(i);
                if (!StringUtils.isEmpty(item.getTitle())) {
                    mOriginalItems.put(item.getTitle(), item);
                }
            }

            menu.clear();
            List internalItems = (List) XposedHelpers.getObjectField(menu, "mItems");
            mOriginal.onPrepareActionMode(mode, menu);
            Iterator<Map.Entry<CharSequence, MenuItem>> it = mOriginalItems.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<CharSequence, MenuItem> kv = it.next();
                boolean exist = false;
                for (int i = 0; i < menu.size(); ++i) {
                    if (kv.getKey().equals(menu.getItem(i).getTitle())) {
                        exist = true;
                        break;
                    }
                }
                if (!exist) {
                    internalItems.add(kv.getValue());
                } else {
                    it.remove();
                }
            }
            XposedHelpers.callMethod(menu, "onItemsChanged", true);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return mOriginalItems.containsKey(item.getTitle()) ? false : mOriginal.onActionItemClicked(mode, item);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mOriginal.onDestroyActionMode(mode);
        }

        @Override
        public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
            if (mOriginal instanceof ActionMode.Callback2) {
                ((ActionMode.Callback2) mOriginal).onGetContentRect(mode, view, outRect);
            }
        }
    }
}
