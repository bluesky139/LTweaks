package li.lingfeng.ltweaks.xposed.shopping;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.utils.ShoppingUtils;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2016/12/19.
 */
@XposedLoad(packages = PackageNames.SMZDM, prefs = R.string.key_smzdm_open_link_in_jd_app)
public class XposedSmzdm extends XposedBase {

    private Activity mInnerBrowser;
    private Activity mKeplerActivity;

    @Override
    public void handleLoadPackage() throws Throwable {
        findAndHookMethod("com.smzdm.client.android.extend.InnerBrowser.InnerBrowserActivity", "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Logger.i("InnerBrowserActivity before onCreate.");
                mInnerBrowser = (Activity) param.thisObject;
            }
        });

        findAndHookMethod("com.kepler.jd.sdk.WebViewActivity", "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Logger.i("kepler WebViewActivity before onCreate.");
                mKeplerActivity = (Activity) param.thisObject;
            }
        });

        findAndHookMethod("com.smzdm.client.android.extend.InnerBrowser.SMZDMWebViewBuilder$SMZDMWebViewClient", "shouldOverrideUrlLoading", WebView.class, String.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("SMZDMWebViewBuilder$SMZDMWebViewClient url " + param.args[1]);
                if (handleUrl((String) param.args[1])) {
                    return true;
                }
                return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
            }
        });

        findAndHookMethod("com.kepler.jd.sdk.JdView$JDBaseWebViewClient", "shouldOverrideUrlLoading", WebView.class, String.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("JdView$JDBaseWebViewClient url " + param.args[1]);
                if (handleUrl((String) param.args[1])) {
                    return true;
                }
                return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
            }
        });

        findAndHookMethod("android.app.Activity", "onDestroy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                if (param.thisObject == mKeplerActivity) {
                    Logger.i("mKeplerActivity onDestroy.");
                    mKeplerActivity = null;
                }
            }
        });
    }

    private boolean handleUrl(String url) {
        if (mInnerBrowser == null) {
            return true;
        }
        String itemId = ShoppingUtils.findItemIdByStore(url, ShoppingUtils.STORE_JD);
        if (itemId != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("openapp.jdmobile://virtual?params={\"category\":\"jump\",\"des\":\"productDetail\",\"skuId\":\"" + itemId + "\",\"sourceType\":\"Item\",\"sourceValue\":\"view-ware\"}"));
            mInnerBrowser.startActivity(intent);
            mInnerBrowser.finish();
            mInnerBrowser = null;
            if (mKeplerActivity != null) {
                mKeplerActivity.finish();
                mKeplerActivity = null;
            }
            Logger.i("InnerBrowser finished.");
            return true;
        }
        return false;
    }
}
