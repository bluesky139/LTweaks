package li.lingfeng.ltweaks.xposed.communication;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.ViewUtils;
import li.lingfeng.ltweaks.xposed.XposedBase;

import static li.lingfeng.ltweaks.utils.ContextUtils.dp2px;

/**
 * Created by lilingfeng on 2017/7/31.
 */
@XposedLoad(packages = {
        PackageNames.QQ_LITE,
        PackageNames.QQ,
        PackageNames.QQ_INTERNATIONAL,
        PackageNames.TIM
}, prefs = R.string.key_qq_collapse_chat_buttons)
public class XposedQQCollaseChatButtons extends XposedBase {

    private static final String CHAT_LIST_VIEW = "com.tencent.mobileqq.bubble.ChatXListView";

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookActivity(ClassNames.QQ_CHAT_ACTIVITY, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final Activity activity = (Activity) param.thisObject;
                new OneActivity(activity);
            }
        });
    }

    private class OneActivity {
        private int mHeight = 0;

        OneActivity(final Activity activity) {
            final ViewGroup rootView = (ViewGroup) activity.findViewById(android.R.id.content);
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (mHeight > 0) {
                        return;
                    }
                    try {
                        handleLayoutChanged(activity);
                    } catch (Throwable e) {
                        Logger.e("Error to handleLayoutChanged, " + e);
                        Logger.stackTrace(e);
                    }
                    if (mHeight > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
            });
        }

        private void handleLayoutChanged(final Activity activity) {
            int idInputBar = ContextUtils.getIdId("inputBar");
            final LinearLayout inputBar = (LinearLayout) activity.findViewById(idInputBar);
            if (inputBar == null) {
                return;
            }
            ViewUtils.traverseViews((ViewGroup) inputBar.getRootView(), new ViewUtils.ViewTraverseCallback2() {
                @Override
                public boolean onView(View view, int deep) {
                    if (view instanceof ImageView) {
                        ImageView imageView = (ImageView) view;
                        if ("表情".equals(imageView.getContentDescription())) {
                            handleButtons(activity, inputBar, (LinearLayout) imageView.getParent());
                            return true;
                        }
                    }
                    return false;
                }
            });
        }

        private void handleButtons(Activity activity, final LinearLayout inputBar, final ViewGroup buttons) {
            if (mHeight <= 0) {
                mHeight = buttons.getMeasuredHeight();
                Logger.d("check height " + mHeight);
                if (mHeight <= 0) {
                    return;
                }
            }
            Logger.i("handleButtons, height " + mHeight);

            ImageView toggleView = new ImageView(activity);
            toggleView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            int padding = dp2px(4);
            toggleView.setPadding(padding, padding, 0, padding);

            Drawable drawable = ContextUtils.createLTweaksContext().getResources().getDrawable(R.drawable.ic_add);
            drawable.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
            toggleView.setImageDrawable(drawable);

            int idSend = ContextUtils.getIdId("fun_btn");
            View sendView = inputBar.findViewById(idSend);
            int sendViewHeight = sendView.getMeasuredHeight();
            Logger.d("sendViewHeight " + sendViewHeight);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(sendViewHeight, sendViewHeight);
            layoutParams.gravity = Gravity.CENTER;
            inputBar.addView(toggleView, inputBar.indexOfChild(sendView), layoutParams);

            toggleButtonsVisibility(false, inputBar, buttons);
            XposedHelpers.callMethod(buttons, "setCustomHeight", 0);
            toggleView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int height = buttons.getMeasuredHeight();
                    Logger.i("toggle buttons, height " + height);
                    try {
                        toggleButtonsVisibility(height == 0, inputBar, buttons);
                    } catch (Throwable e) {
                        Logger.e("Error to toggleButtonsVisibility, " + e);
                        Logger.stackTrace(e);
                    }
                }
            });
        }

        private void toggleButtonsVisibility(boolean visible, final LinearLayout inputBar, final ViewGroup buttons) {
            XposedHelpers.callMethod(buttons, "setCustomHeight", visible ? mHeight : 0);
            Class clsChatListView = findClass(CHAT_LIST_VIEW);
            View listView = ViewUtils.findViewByType((ViewGroup) inputBar.getParent(), clsChatListView);
            RelativeLayout.LayoutParams listLayoutParams = (RelativeLayout.LayoutParams) listView.getLayoutParams();
            listLayoutParams.bottomMargin = visible ? dp2px(90) : dp2px(50);
        }
    }
}
