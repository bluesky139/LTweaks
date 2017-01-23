package li.lingfeng.ltweaks.xposed.entertainment;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/1/7.
 */
@XposedLoad(packages = "tv.danmaku.bili", prefs = R.string.key_bilibili_subscriptions_goto_top)
public class XposedBilibili extends XposedBase {

    private View mRecyclerView;
    private Method mMethodSmoothScrollToPosition;
    private boolean mInMainActivity = false;
    private XC_MethodHook.Unhook mHookedTabClick;

    @Override
    public void handleLoadPackage() throws Throwable {
        /*findAndHookMethod("tv.danmaku.bili.ui.main.HomeFragment$1", lpparam.classLoader, "onPageSelected", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Logger.d("HomeFragment$1 onPageSelected " + param.args[0]);
            }
        });*/


        Class clsRecyclerView = XposedHelpers.findClass("android.support.v7.widget.RecyclerView", lpparam.classLoader);
        mMethodSmoothScrollToPosition = clsRecyclerView.getDeclaredMethod("smoothScrollToPosition", int.class);

        Class clsAttentionFragment = XposedHelpers.findClass("tv.danmaku.bili.ui.main.attention.AttentionDynamicFragment", lpparam.classLoader);
        Method[] methods = clsAttentionFragment.getDeclaredMethods();
        String methodName = null;
        for (Method method : methods) {
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length == 2 && paramTypes[0] == clsRecyclerView && paramTypes[1] == Bundle.class) {
                methodName = method.getName();
                Logger.i("Got method with params(RecyclerView, Bundle) in AttentionDynamicFragment");
                break;
            }
        }

        findAndHookMethod("tv.danmaku.bili.ui.main.attention.AttentionDynamicFragment", methodName, clsRecyclerView, Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mRecyclerView = (View) param.args[0];
                Logger.i("Got AttentionDynamicFragment RecyclerView.");
            }
        });

        findAndHookMethod("tv.danmaku.bili.ui.main.attention.AttentionDynamicFragment", "onDestroy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mRecyclerView = null;
                Logger.i("AttentionDynamicFragment is destroyed.");
            }
        });

        findAndHookMethod("android.app.Activity", "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.thisObject.getClass().getName().equals("tv.danmaku.bili.MainActivity")) {
                    Logger.i("In MainActivity.");
                    mInMainActivity = true;
                }
            }
        });

        findAndHookMethod("android.app.Activity", "onResume", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.thisObject.getClass().getName().equals("tv.danmaku.bili.MainActivity")) {
                    Logger.i("In MainActivity.");
                    mInMainActivity = true;
                }
            }
        });

        findAndHookMethod("android.app.Activity", "onPause", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.thisObject.getClass().getName().equals("tv.danmaku.bili.MainActivity")) {
                    Logger.i("In MainActivity.");
                    mInMainActivity = false;
                }
            }
        });

        findAndHookMethod("android.app.Activity", "onDestroy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.thisObject.getClass().getName().equals("tv.danmaku.bili.MainActivity")) {
                    Logger.i("MainActivity onDestroy.");
                    mInMainActivity = false;
                    mHookedTabClick.unhook();
                    mHookedTabClick = null;
                }
            }
        });

        findAndHookConstructor("tv.danmaku.bili.widget.PagerSlidingTabStrip", Context.class, AttributeSet.class, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!mInMainActivity || mHookedTabClick != null) {
                    return;
                }

                Field[] fields = param.thisObject.getClass().getDeclaredFields();
                View.OnClickListener tabClickListener = null;
                for (Field field : fields) {
                    if (field.getType() == View.OnClickListener.class) {
                        field.setAccessible(true);
                        tabClickListener = (View.OnClickListener) field.get(param.thisObject);
                        Logger.i("Got tab click listener from PagerSlidingTabStrip.");
                        hookTabClick(param.thisObject, tabClickListener);
                        break;
                    }
                }
            }
        });
    }

    private void hookTabClick(final Object thisTabStrip, final View.OnClickListener tabClickListener) {
        mHookedTabClick = findAndHookMethod(tabClickListener.getClass(), "onClick", View.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!mInMainActivity || mRecyclerView == null) {
                    return;
                }

                View view = (View) param.args[0];
                if (!(view instanceof TextView)) {
                    return;
                }

                Field[] fields = thisTabStrip.getClass().getDeclaredFields();
                Object viewPager = null;
                for (Field field : fields) {
                    if (field.getType().getName().equals("android.support.v4.view.ViewPager")) {
                        field.setAccessible(true);
                        viewPager = field.get(thisTabStrip);
                        break;
                    }
                }
                if (viewPager == null) {
                    return;
                } else {
                    Logger.i("Got view pager from PagerSlidingTabStrip.");
                }

                Method methodGetCurrentItem = viewPager.getClass().getDeclaredMethod("getCurrentItem");
                int currentItem = (int) methodGetCurrentItem.invoke(viewPager);
                int clickItem = (int) view.getTag();
                Logger.d("currentItme " + currentItem + ", clickItem " + clickItem);
                if (currentItem != clickItem) {
                    return;
                }

                TextView textView = (TextView) view;
                int stringId = view.getContext().getResources().getIdentifier("main_page_attentions", "string", "tv.danmaku.bili");
                String subscriptionString = view.getContext().getString(stringId);
                if (!subscriptionString.toUpperCase().equals(textView.getText().toString().toUpperCase())) {
                    return;
                }

                Logger.i("Scroll RecyclerView to top in AttentionDynamicFragment.");
                mMethodSmoothScrollToPosition.invoke(mRecyclerView, 0);
            }
        });
    }
}
