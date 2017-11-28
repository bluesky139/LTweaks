package li.lingfeng.ltweaks.xposed.communication;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/11/28.
 */
@XposedLoad(packages = {
        PackageNames.QQ_LITE,
        PackageNames.QQ,
        PackageNames.QQ_INTERNATIONAL,
        PackageNames.TIM
}, prefs = R.string.key_qq_prevent_audio_panel)
public class XposedQQPreventAudioPanel extends XposedBase {

    private static final String CHAT_PIE = "com.tencent.mobileqq.activity.BaseChatPie";

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(CHAT_PIE, "showAudioPanelIfNeed", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("Prevent showAudioPanelIfNeed()");
                param.setResult(null);
            }
        });
    }
}
