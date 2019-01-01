package li.lingfeng.ltweaks.xposed.system;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

@XposedLoad(packages = {}, prefs = R.string.key_text_long_press_to_copy, excludedPackages = {
        PackageNames.ANDROID, PackageNames.ANDROID_SYSTEM_UI
})
public class XposedTextLongPressToCopy extends XposedBase {

    private static final int MAX_DOWN_OFFSET = 15;
    private LongPressRunnable mLongPressRunnable;
    private float mDownX;
    private float mDownY;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(TextView.class, "onTouchEvent", MotionEvent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                final TextView textView = (TextView) param.thisObject;
                if (textView instanceof EditText || textView instanceof Button || textView.isTextSelectable()) {
                    return;
                }
                MotionEvent event = (MotionEvent) param.args[0];
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mDownX = event.getRawX();
                        mDownY = event.getRawY();
                        pendingLongPress(textView);
                        break;
                }
            }
        });

        findAndHookMethod(View.class, "dispatchPointerEvent", MotionEvent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                MotionEvent event = (MotionEvent) param.args[0];
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        cancelLongPress();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(mDownX - event.getRawX()) > MAX_DOWN_OFFSET || Math.abs(mDownY - event.getRawY()) > MAX_DOWN_OFFSET) {
                            cancelLongPress();
                        }
                        break;
                }
            }
        });
    }

    private void pendingLongPress(TextView textView) {
        cancelLongPress();
        mLongPressRunnable = new LongPressRunnable(textView);
        textView.postDelayed(mLongPressRunnable, 400);
    }

    private void cancelLongPress() {
        if (mLongPressRunnable != null) {
            mLongPressRunnable.selfRemove();
            mLongPressRunnable = null;
        }
    }

    class LongPressRunnable implements Runnable {

        private TextView textView;

        public LongPressRunnable(TextView textView) {
            this.textView = textView;
        }

        @Override
        public void run() {
            Logger.v("Long press to copy text.");
            ClipboardManager clipboardManager = (ClipboardManager) textView.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboardManager.setPrimaryClip(ClipData.newPlainText(null, textView.getText()));
            mLongPressRunnable = null;
        }

        public void selfRemove() {
            textView.removeCallbacks(this);
        }
    }
}
