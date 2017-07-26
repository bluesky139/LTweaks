package li.lingfeng.ltweaks.xposed.system;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.MyApplication;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.Utils;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/6/29.
 */
@XposedLoad(packages = {
        PackageNames.CHROME,
        PackageNames.CHROME_BETA,
        PackageNames.CHROME_CANARY,
        PackageNames.CHROME_DEV
}, prefs = R.string.key_native_clipboard_fix_chrome)
public class XposedNativeClipboard extends XposedBase {

    private static final String SELECTION_POPUP_CONTROLLER = "org.chromium.content.browser.SelectionPopupController";
    private static final String ACTION_MODE_CALLBACK = "org.chromium.content.browser.input.FloatingPastePopupMenu$ActionModeCallback";
    private static final String CLIPBOARD_STR = "Clipboard";

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(SELECTION_POPUP_CONTROLLER, "onCreateActionMode", ActionMode.class, Menu.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                hookOnCreateActionMode(param);
            }
        });

        findAndHookMethod(SELECTION_POPUP_CONTROLLER, "onActionItemClicked", ActionMode.class, MenuItem.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                hookOnActionItemClicked(param);
            }
        });

        findAndHookMethod(ACTION_MODE_CALLBACK, "onCreateActionMode", ActionMode.class, Menu.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                hookOnCreateActionMode(param);
            }
        });

        findAndHookMethod(ACTION_MODE_CALLBACK, "onActionItemClicked", ActionMode.class, MenuItem.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                hookOnActionItemClicked(param);
            }
        });
    }

    private void hookOnCreateActionMode(final XC_MethodHook.MethodHookParam param) {
        Menu menu = (Menu) param.args[1];
        final int idMenuPaste = ContextUtils.getIdId("select_action_menu_paste");
        if (Utils.findMenuItemById(menu, idMenuPaste) != null) {
            Logger.i("Create menu " + CLIPBOARD_STR);
            menu.add(CLIPBOARD_STR);
        }
    }

    private void hookOnActionItemClicked(final XC_MethodHook.MethodHookParam param) {
        MenuItem item = (MenuItem) param.args[1];
        if (!CLIPBOARD_STR.equals(item.getTitle())) {
            return;
        }

        Logger.i(CLIPBOARD_STR + " is clicked.");
        ActionMode mode = (ActionMode) param.args[0];
        final int idMenuPaste = ContextUtils.getIdId("select_action_menu_paste");
        final MenuItem pasteItem = Utils.findMenuItemById(mode.getMenu(), idMenuPaste);

        Intent intent = new Intent();
        intent.setClassName("com.dhm47.nativeclipboard", "com.dhm47.nativeclipboard.ClipBoardA");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        MyApplication.instance().startActivity(intent);

        final ActionMode actionMode = (ActionMode) param.args[0];
        final ClipboardManager clipboardManager = (ClipboardManager) MyApplication.instance().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardManager.addPrimaryClipChangedListener(new ClipboardManager.OnPrimaryClipChangedListener() {
            @Override
            public void onPrimaryClipChanged() {
                try {
                    clipboardManager.removePrimaryClipChangedListener(this);
                    if (clipboardManager.getPrimaryClip().getItemAt(0).coerceToText(MyApplication.instance()).toString().equals("//NATIVECLIPBOARDCLOSE//")) {
                        Logger.i("No paste from native clipboard.");
                        actionMode.finish();
                    } else {
                        Logger.i("Paste from native clipboard.");
                        param.args[1] = pasteItem;
                        XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                    }
                } catch (Throwable e) {
                    Logger.e("Paste from native clipboard error, " + e);
                    Toast.makeText(MyApplication.instance(), "Paste from native clipboard error.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        param.setResult(true);
    }
}
