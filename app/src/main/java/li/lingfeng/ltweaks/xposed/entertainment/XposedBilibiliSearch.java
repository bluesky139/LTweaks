package li.lingfeng.ltweaks.xposed.entertainment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.ViewUtils;
import li.lingfeng.ltweaks.xposed.XposedCommon;

/**
 * Created by lilingfeng on 2017/7/3.
 */
@XposedLoad(packages = {
        PackageNames.ANDROID,
        PackageNames.BILIBILI,
        PackageNames.BILIBILI_IN
}, prefs = R.string.key_bilibili_search)
public class XposedBilibiliSearch extends XposedCommon {
    @Override
    protected void handleLoadPackage() throws Throwable {
        if (lpparam.packageName.equals(PackageNames.ANDROID)) {
            hookAndSetComponentExported(PackageNames.BILIBILI_IN, ClassNames.BILIBILI_SEARCH_ACTIVITY);
            hookAndSetComponentExported(PackageNames.BILIBILI, ClassNames.BILIBILI_SEARCH_ACTIVITY);
            return;
        }

        findAndHookActivity(ClassNames.BILIBILI_SEARCH_ACTIVITY, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                Intent intent = activity.getIntent();
                if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
                    String text = intent.getStringExtra("query");
                    if (text != null) {
                        Logger.i("Search " + text);
                        EditText editText = (EditText) ViewUtils.findViewByName(activity, "search_src_text");
                        editText.setText(text);
                        editText.onEditorAction(EditorInfo.IME_ACTION_DONE);
                    }
                }
            }
        });
    }
}
