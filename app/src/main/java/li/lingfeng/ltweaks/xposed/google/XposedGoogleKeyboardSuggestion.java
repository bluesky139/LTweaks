package li.lingfeng.ltweaks.xposed.google;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

import static android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;

/**
 * Created by lilingfeng on 2017/7/27.
 */
@XposedLoad(packages = PackageNames.GOOGLE, prefs = R.string.key_google_keyboard_suggestion)
public class XposedGoogleKeyboardSuggestion extends XposedBase {
    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookConstructor(EditText.class, Context.class, AttributeSet.class, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final int idSearchBox = ContextUtils.getIdId("search_box");
                final EditText editText = (EditText) param.thisObject;
                final int inputType = editText.getInputType();
                if (editText.getId() == idSearchBox && (inputType & TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0) {
                    Logger.i("Constructor allow keyboard suggestion.");
                    editText.setInputType(inputType & ~TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                }
            }
        });

        findAndHookMethod(TextView.class, "setInputType", int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!(param.thisObject instanceof EditText)) {
                    return;
                }
                final int idSearchBox = ContextUtils.getIdId("search_box");
                final EditText editText = (EditText) param.thisObject;
                final int inputType = (int) param.args[0];
                if (editText.getId() == idSearchBox && (inputType & TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0) {
                    Logger.i("setInputType Allow keyboard suggestion.");
                    param.args[0] = inputType & ~TYPE_TEXT_FLAG_NO_SUGGESTIONS;
                }
            }
        });
    }
}
