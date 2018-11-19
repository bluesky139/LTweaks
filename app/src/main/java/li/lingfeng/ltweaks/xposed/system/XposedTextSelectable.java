package li.lingfeng.ltweaks.xposed.system;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/11/9.
 */
@XposedLoad(packages = {}, prefs = R.string.key_text_double_click_for_selectable)
public class XposedTextSelectable extends XposedBase {
    @Override
    protected void handleLoadPackage() throws Throwable {
        if (lpparam.packageName.equals(PackageNames.ANDROID)
                || lpparam.packageName.equals(PackageNames.ANDROID_SYSTEM_UI)
                || lpparam.packageName.equals(PackageNames.DIALER)
                || lpparam.packageName.equals(PackageNames.GOOGLE_DIALER)) {
            return;
        }

        findAndHookConstructor(TextView.class, Context.class, AttributeSet.class, int.class, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                hookTextView(param);
            }
        });
    }

    private void hookTextView(XC_MethodHook.MethodHookParam param) {
        final TextView textView = (TextView) param.thisObject;
        if (textView instanceof EditText || textView instanceof Button) {
            return;
        }
        textView.setOnTouchListener(new View.OnTouchListener() {
            int touchCount = 0;
            long lastTime = 0;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (System.currentTimeMillis() - lastTime > 300) {
                        touchCount = 0;
                    }
                    ++touchCount;
                    lastTime = System.currentTimeMillis();
                    if (touchCount == 2) {
                        textView.setTextIsSelectable(true);
                        Toast.makeText(textView.getContext(), "Select now", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }
                return false;
            }
        });
    }
}
