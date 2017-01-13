package li.lingfeng.ltweaks.xposed;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

/**
 * Created by smallville on 2017/1/4.
 */
@XposedLoad(packages = "com.arjerine.textxposed", prefs = R.string.key_text_aide_open_youdao)
public class XposedTextAide implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        findAndHookMethod("com.arjerine.textxposed.define.DispPopup", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Activity activity = ((Activity) param.thisObject);
                Intent intent = activity.getIntent();
                /*for (String key : intent.getExtras().keySet()) {
                    Log.d("Xposed", "extra " + key + " -> " + intent.getStringExtra(key));
                }*/

                String word = intent.getStringExtra("word");
                if (word == null)
                    word = intent.getStringExtra("android.intent.extra.PROCESS_TEXT");
                if (word == null)
                    word = intent.getStringExtra("android.intent.extra.TEXT");
                Logger.d("Text Aide word " + word);

                intent = new Intent();
                intent.setClassName("com.youdao.dict", "com.youdao.dict.activity.QuickDictQueryActivity");
                intent.putExtra("query", word);
                activity.startActivity(intent);
                activity.finish();
            }
        });
    }
}
