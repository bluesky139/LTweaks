package li.lingfeng.ltweaks.xposed;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.utils.Logger;

import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

/**
 * Created by smallville on 2017/1/15.
 */
@XposedLoad(packages = "com.tencent.mm", prefs = R.string.key_wechat_system_share_in_inner_browser)
public class XposedWeChat implements IXposedHookLoadPackage {

    private Class<?> mClsWebViewUI;

    // Comments in v6.5.3
    private Class<?> mClsMenuItemClick; // com.tencent.mm.ui.tools.l
    private Class<?> mClsContextMenu;   // com.tencent.mm.ui.base.l
    private Method mMethodAddMenuItem;  // a(int, CharSequence, int) from com.tencent.mm.ui.base.l

    private Activity mWebViewUI;
    private MenuItem mMenuItemCopy;
    private int mIconCopy;
    private int mPosCopy = -1;
    private boolean mShareClicked = false;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        mClsWebViewUI = XposedHelpers.findClass("com.tencent.mm.plugin.webview.ui.tools.WebViewUI", lpparam.classLoader);
        Field[] fieldsWebViewUI = mClsWebViewUI.getDeclaredFields();
        for (Field fieldWebViewUI : fieldsWebViewUI) {
            if (!fieldWebViewUI.getType().getName().startsWith("com.tencent.mm.ui.tools.")
                    || !AdapterView.OnItemClickListener.class.isAssignableFrom(fieldWebViewUI.getType())) {
                continue;
            }

            Field[] fields = fieldWebViewUI.getType().getDeclaredFields();
            for (Field field : fields) {
                if (!field.getType().getName().startsWith("com.tencent.mm.ui.base.")
                        || !ContextMenu.class.isAssignableFrom(field.getType())) {
                    continue;
                }

                Method[] methods = field.getType().getDeclaredMethods();
                for (Method method : methods) {
                    if (!Modifier.isStatic(method.getModifiers())
                            && Modifier.isPublic(method.getModifiers())
                            && Modifier.isFinal(method.getModifiers())
                            && method.getParameterTypes().length == 3
                            && method.getParameterTypes()[0] == int.class
                            && method.getParameterTypes()[1] == CharSequence.class
                            && method.getParameterTypes()[2] == int.class
                            && method.getReturnType() == MenuItem.class) {
                        mMethodAddMenuItem = method;
                        Logger.i("Got mMethodAddMenuItem " + mMethodAddMenuItem);
                        break;
                    }
                }

                if (mMethodAddMenuItem != null) {
                    mClsContextMenu = field.getType();
                    Logger.i("Got mClsContextMenu " + mClsContextMenu);
                    break;
                }
            }

            if (mClsContextMenu != null) {
                break;
            }
        }

        if (mClsContextMenu == null) {
            Logger.e("Can't find mMethodAddMenuItem.");
            return;
        }

        Class<?> clsAppBrandPageActionSheet = XposedHelpers.findClass("com.tencent.mm.plugin.appbrand.page.AppBrandPageActionSheet", lpparam.classLoader);
        Field[] fields = clsAppBrandPageActionSheet.getDeclaredFields();
        for (Field field : fields) {
            if (!field.getType().getName().startsWith("com.tencent.mm.ui.widget.")
                    || !ViewTreeObserver.OnGlobalLayoutListener.class.isAssignableFrom(field.getType())) {
                continue;
            }

            for (int i = 1; i <= 5; ++i) {
                Class<?> cls = XposedHelpers.findClass(field.getType().getName() + "$" + i, lpparam.classLoader);
                if (cls == null) {
                    break;
                }
                if (AdapterView.OnItemClickListener.class.isAssignableFrom(cls)) {
                    mClsMenuItemClick = cls;
                    Logger.i("Got mClsMenuItemClick " + mClsMenuItemClick);
                    break;
                }
            }

            if (mClsMenuItemClick != null) {
                break;
            }
        }

        if (mClsMenuItemClick == null) {
            Logger.e("Can't find mClsMenuItemClick.");
            return;
        }

        findAndHookMethod(mClsWebViewUI, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mWebViewUI = (Activity) param.thisObject;
                mShareClicked = false;
                Logger.d("In WebViewUI.");
            }
        });

        findAndHookMethod(mClsWebViewUI, "onDestroy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mWebViewUI = null;
                mMenuItemCopy = null;
                mShareClicked = false;
                Logger.d("Not in WebViewUI.");
            }
        });

        findAndHookConstructor(mClsContextMenu, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (mWebViewUI == null) {
                    return;
                }
                mPosCopy = -1;
                mMenuItemCopy = null;
                Logger.d("Creating menu.");
            }
        });

        findAndHookMethod(mClsContextMenu, mMethodAddMenuItem.getName(), int.class, CharSequence.class, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (mWebViewUI == null || mMenuItemCopy != null) {
                    return;
                }

                ++mPosCopy;
                if ((int) param.args[0] == 6) {
                    mMenuItemCopy = (MenuItem) param.getResult();
                    mIconCopy = (int) param.args[2];
                    Logger.i("Got menu item of copy, pos " + mPosCopy);
                    String lang = mWebViewUI.getResources().getConfiguration().locale.getLanguage();
                    mMethodAddMenuItem.invoke(param.thisObject, 999, lang.equals("zh") ? "分享..." : "Share...", mIconCopy);
                    mShareClicked = false;
                }
            }
        });

        findAndHookMethod(mClsMenuItemClick, "onItemClick", AdapterView.class, View.class, int.class, long.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mWebViewUI == null) {
                    return;
                }
                if ((int) param.args[2] == mPosCopy + 1) {
                    param.args[2] = mPosCopy;
                    mShareClicked = true;
                    Logger.i("Share is clicked.");
                }
            }
        });

        findAndHookMethod(ClipboardManager.class, "setText", CharSequence.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mWebViewUI == null || !mShareClicked) {
                    return;
                }
                String url = param.args[0].toString();
                Logger.i("Got url " + url);

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, url);
                mWebViewUI.startActivity(intent);
                mShareClicked = false;
            }
        });
    }
}
