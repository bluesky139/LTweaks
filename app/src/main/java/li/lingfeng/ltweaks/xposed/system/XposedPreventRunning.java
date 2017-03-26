package li.lingfeng.ltweaks.xposed.system;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.utils.IOUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/3/26.
 */

public class XposedPreventRunning extends XposedBase {

    protected static List<String> sPreventList = new ArrayList<>();
    protected static List<Integer> sPreventUids = new ArrayList<>();

    @Override
    protected void handleLoadPackage() throws Throwable {
        if (sPreventList.size() == 0) {
            final List<String> lines = IOUtils.readLines("/data/system/me.piebridge.prevent.list");
            for (String line : lines) {
                Logger.d("Prevent list item: " + line);
            }
            sPreventList = lines;
        }

        findAndHookMethod(ClassNames.ACTIVITY_MANAGER_SERVICE, "finishBooting", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (sPreventUids.size() > 0) {
                    return;
                }
                Field field = param.thisObject.getClass().getDeclaredField("mContext");
                field.setAccessible(true);
                Context context = (Context) field.get(param.thisObject);

                for (String line : sPreventList) {
                    try {
                        ApplicationInfo info = context.getPackageManager().getApplicationInfo(line, PackageManager.GET_META_DATA);
                        sPreventUids.add(info.uid);
                        Logger.d("Prevent list item uid: " + info.uid);
                    } catch (Exception e)
                    {}
                }
            }
        });
    }
}
