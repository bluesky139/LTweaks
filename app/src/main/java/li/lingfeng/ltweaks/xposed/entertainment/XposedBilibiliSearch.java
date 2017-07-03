package li.lingfeng.ltweaks.xposed.entertainment;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedCommon;

/**
 * Created by lilingfeng on 2017/7/3.
 */
@XposedLoad(packages = { PackageNames.ANDROID, PackageNames.BILIBILI }, prefs = R.string.key_bilibili_search)
public class XposedBilibiliSearch extends XposedCommon {
    @Override
    protected void handleLoadPackage() throws Throwable {
        if (lpparam.packageName.equals(PackageNames.ANDROID)) {
            hookAndSetComponentExported(PackageNames.BILIBILI, ClassNames.BILIBILI_SEARCH_ACTIVITY);
        } else {
            // This hook should be loaded in all packages, but I don't know if there is any side-effect.
            findAndHookMethod(TextView.class, "canProcessText", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Logger.i("canProcessText return true");
                    param.setResult(true);
                }
            });
        }
    }
}
