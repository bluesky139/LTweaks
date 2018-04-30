package li.lingfeng.ltweaks.xposed.entertainment;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.AttributeSet;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2018/2/25.
 */
@XposedLoad(packages = {
        PackageNames.BILIBILI,
        PackageNames.BILIBILI_IN
}, prefs = R.string.key_bilibili_darken)
public class XposedBilibiliDarkenReadingPageLoading extends XposedBase {

    private boolean mHookedJsInjectionClass = false;
    private WeakHashMap<Object, WebView> mWebViews;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookConstructor(WebView.class, Context.class, AttributeSet.class, int.class, int.class, Map.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                WebView webView = (WebView) param.thisObject;
                webView.setBackgroundColor(Color.parseColor("#2B2B2B"));

                /*String cssOverride = "body { background : #2B2B2B; }";
                String content =
                        "<html>" +
                                "<head>" +
                                "<meta content=\"text/html; charset=utf-8\" http-equiv=\"content-type\">" +
                                "<meta name=\"viewport\" content=\"width=device-width, user-scalable=no\" />" +
                                "<style type=\"text/css\">" +
                                "body { padding : 0px; margin : 0px; line-height : 130%; }" +
                                "img, video, iframe { max-width : 100%; width : auto; height : auto; }" +
                                " table { width : 100%; }" +
                                cssOverride +
                                "</style>" +
                                "</head>" +
                                "<body>" + "</body></html>";
                webView.loadData(content, "text/html", "utf-8");*/
            }
        });

        findAndHookMethod(WebView.class, "loadUrl", String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                handleLoadUrl(param);
            }
        });

        findAndHookMethod(WebView.class, "loadUrl", String.class, Map.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                handleLoadUrl(param);
            }
        });

        findAndHookMethod(WebView.class, "addJavascriptInterface", Object.class, String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object jsInjectionObject = param.args[0];
                if (!mHookedJsInjectionClass) {
                    try {
                        Method method = jsInjectionObject.getClass().getDeclaredMethod("postMessage", String.class);
                        if (method.getAnnotation(JavascriptInterface.class) != null) {
                            hookJsInjectionClass(method);
                            mHookedJsInjectionClass = true;
                            mWebViews = new WeakHashMap<>();
                        }
                    } catch (NoSuchMethodException e) {}
                }
                if (mHookedJsInjectionClass) {
                    WebView webView = (WebView) param.thisObject;
                    mWebViews.put(jsInjectionObject, webView);
                    Logger.d("jsInjectionObject " + jsInjectionObject + " -> webView " + webView);
                }
            }
        });
    }

    private void handleLoadUrl(XC_MethodHook.MethodHookParam param) {
        String url = (String) param.args[0];
        if (url.startsWith("https://www.bilibili.com/read/")) {
            WebView webView = (WebView) param.thisObject;
            webView.setAlpha(0);
            Logger.d("webView " + webView + " set alpha 0.");
        }
    }

    private void hookJsInjectionClass(Method method) {
        Logger.v("Hook js injection class " + method);
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                String jsCallbackString = (String) param.args[0];
                if (jsCallbackString.contains("\"method\":\"biliapp.success\",\"data\":\"window._biliapp.callback\"")) {
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            Object jsInjectionObject = param.thisObject;
                            WebView webView = mWebViews.get(jsInjectionObject);
                            if (webView != null) {
                                webView.setAlpha(1);
                                Logger.d("webView " + webView + " set alpha 1.");
                            }
                        }
                    });
                }
            }
        });
    }
}
