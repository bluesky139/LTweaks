package li.lingfeng.ltweaks.xposed.entertainment;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

@XposedLoad(packages = {
        PackageNames.BILIBILI,
        PackageNames.BILIBILI_IN
}, prefs = R.string.key_bilibili_dismiss_update)
public class XposedBilibiliDismissUpdate extends XposedBase {

    private static final String UPDATE_VER_INFO = "tv.danmaku.bili.update.BiliUpdateVerInfo";

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookConstructor(UPDATE_VER_INFO, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.d("BiliUpdateVerInfo construct.");
                param.setThrowable(new Throwable("Dismiss update"));
            }
        });
    }
}
