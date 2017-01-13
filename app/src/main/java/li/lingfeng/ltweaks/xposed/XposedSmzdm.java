package li.lingfeng.ltweaks.xposed;

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
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

/**
 * Created by smallville on 2016/12/19.
 */
@XposedLoad(packages = "com.smzdm.client.android", prefs = R.string.key_smzdm_open_link_in_jd_app)
public class XposedSmzdm implements IXposedHookLoadPackage {

    private Activity mInnerBrowser;
    private Activity mKeplerActivity;
    private Pattern[] mPatterns = {
            Pattern.compile("https?://re\\.jd\\.com/cps/item/(\\d+)\\.html"),
            Pattern.compile("https?://item\\.jd\\.com/(\\d+)\\.html"),
            //Pattern.compile("https?://.*\\.jd\\.com/.*sku%3D(\\d+)"),
            //Pattern.compile("https?://.*\\.jd\\.com/.*sku=(\\d+)"),
            //Pattern.compile("https?://.*\\.jd\\.com/.*/product/(\\d+)"),
            //Pattern.compile("https?://.*\\.jd\\.com/.*%2Fproduct%2F(\\d+)"),
            Pattern.compile("https?://.*\\.jd\\.com(/.*)?(/|%2F|\\?|%3F)(product|sku)(/|%2F|=|%3D)(\\d+)")
    };

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        findAndHookMethod("com.smzdm.client.android.extend.InnerBrowser.InnerBrowserActivity", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Logger.d("InnerBrowserActivity before onCreate.");
                mInnerBrowser = (Activity) param.thisObject;
            }
        });

        findAndHookMethod("com.kepler.jd.sdk.WebViewActivity", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Logger.d("kepler WebViewActivity before onCreate.");
                mKeplerActivity = (Activity) param.thisObject;
            }
        });

        findAndHookMethod("com.smzdm.client.android.extend.InnerBrowser.SMZDMWebViewBuilder$SMZDMWebViewClient", lpparam.classLoader, "shouldOverrideUrlLoading", WebView.class, String.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                Logger.d("SMZDMWebViewBuilder$SMZDMWebViewClient url " + param.args[1]);
                if (handleUrl((String) param.args[1])) {
                    return true;
                }
                return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
            }
        });

        findAndHookMethod("com.kepler.jd.sdk.JdView$JDBaseWebViewClient", lpparam.classLoader, "shouldOverrideUrlLoading", WebView.class, String.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                Logger.d("JdView$JDBaseWebViewClient url " + param.args[1]);
                if (handleUrl((String) param.args[1])) {
                    return true;
                }
                return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
            }
        });

        findAndHookMethod("android.app.Activity", lpparam.classLoader, "onDestroy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                if (param.thisObject == mKeplerActivity) {
                    Logger.d("mKeplerActivity onDestroy.");
                    mKeplerActivity = null;
                }
            }
        });
    }

    private boolean handleUrl(String url) {
        if (mInnerBrowser == null) {
            return true;
        }
        for (Pattern pattern : mPatterns) {
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                String itemId = matcher.group(matcher.groupCount());
                Logger.i("Got jd item id " + itemId);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("openapp.jdmobile://virtual?params={\"category\":\"jump\",\"des\":\"productDetail\",\"skuId\":\"" + itemId + "\",\"sourceType\":\"Item\",\"sourceValue\":\"view-ware\"}"));
                mInnerBrowser.startActivity(intent);
                mInnerBrowser.finish();
                mInnerBrowser = null;
                if (mKeplerActivity != null) {
                    mKeplerActivity.finish();
                    mKeplerActivity = null;
                }
                Logger.d("InnerBrowser finished.");
                return true;
            }
        }
        return false;
    }
}
