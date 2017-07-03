package li.lingfeng.ltweaks.xposed.entertainment;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.xposed.XposedCommon;

/**
 * Created by lilingfeng on 2017/7/3.
 */
@XposedLoad(packages = PackageNames.ANDROID, prefs = R.string.key_bilibili_search)
public class XposedBilibiliSearch extends XposedCommon {
    @Override
    protected void handleLoadPackage() throws Throwable {
        hookAndSetComponentExported(PackageNames.BILIBILI, ClassNames.BILIBILI_SEARCH_ACTIVITY);
    }
}
