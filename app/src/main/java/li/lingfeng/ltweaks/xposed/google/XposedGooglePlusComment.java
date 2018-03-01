package li.lingfeng.ltweaks.xposed.google;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.SimpleFloatingButton;
import li.lingfeng.ltweaks.utils.ViewUtils;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/6/14.
 */
@XposedLoad(packages = PackageNames.GOOGLE_PLUS, prefs = R.string.key_google_plus_hide_comment_edit)
public class XposedGooglePlusComment extends XposedBase {

    private static final String sOneActivity = "com.google.android.apps.plus.stream.oneup.OneUpStreamActivity";

    private View mFooter;
    private SimpleFloatingButton mFloatingButton;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookActivity(sOneActivity, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (mFooter != null) {
                    return;
                }

                final Activity activity = (Activity) param.thisObject;
                mFooter = activity.findViewById(ContextUtils.getIdId("comment_fragment_container"));
                if (mFooter == null) {
                    Logger.e("mFooter is null, mFooter " + mFooter);
                    return;
                }

                mFloatingButton = SimpleFloatingButton.make(activity);
                mFloatingButton.setBackgroundColor(Color.parseColor("#FFDB4437"));
                Drawable commentDrawable = ContextUtils.getDrawable("quantum_ic_comment_white_24");
                mFloatingButton.setImageDrawable(commentDrawable);
                mFloatingButton.setAnimatedType(SimpleFloatingButton.ANIMATED_TRANSLATION_Y);
                mFloatingButton.show();
                Logger.i("Floating button is created.");

                mFooter.setVisibility(View.GONE);
                mFloatingButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Logger.i("Floating button is clicked.");
                        mFooter.setVisibility(View.VISIBLE);
                        View editText = activity.findViewById(ContextUtils.getIdId("comment_edit_text"));
                        editText.requestFocus();
                        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.showSoftInput(editText, 0);
                    }
                });
            }
        });

        findAndHookMethod(View.class, "requestFocus", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mFooter == null) {
                    return;
                }
                View view = (View) param.thisObject;
                if (view instanceof EditText && view.getId() == ContextUtils.getIdId("comment_edit_text")) {
                    Logger.i("footer_text_with_embeds requestFocus().");
                    mFooter.setVisibility(View.VISIBLE);
                    if (mFloatingButton != null) {
                        mFloatingButton.destroy();
                        mFloatingButton = null;
                    }
                }
            }
        });

        findAndHookActivity(sOneActivity, "dispatchTouchEvent", MotionEvent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mFloatingButton != null) {
                    mFloatingButton.getActivityTouchEventListener().onDispatch((MotionEvent) param.args[0]);
                }
            }
        });

        findAndHookActivity(sOneActivity, "onDestroy", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (mFloatingButton != null) {
                    mFloatingButton.destroy();
                    mFloatingButton = null;
                }
                mFooter = null;
            }
        });
    }
}
