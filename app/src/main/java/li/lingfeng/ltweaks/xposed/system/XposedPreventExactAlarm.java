package li.lingfeng.ltweaks.xposed.system;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.IOUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/3/25.
 */
@XposedLoad(packages = PackageNames.ANDROID, prefs = R.string.key_prevent_running_prevent_exact_alarm)
public class XposedPreventExactAlarm extends XposedBase {

    private static final long WINDOW_EXACT = 0;
    private static final long WINDOW_HEURISTIC = -1;
    private static final int FLAG_ALLOW_WHILE_IDLE = 1<<2;

    private List<Integer> mUids = new ArrayList<>();

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(ClassNames.ACTIVITY_MANAGER_SERVICE, "finishBooting", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Field field = param.thisObject.getClass().getDeclaredField("mContext");
                field.setAccessible(true);
                Context context = (Context) field.get(param.thisObject);

                final List<String> lines = IOUtils.readLines("/data/system/me.piebridge.prevent.list");
                for (String line : lines) {
                    try {
                        ApplicationInfo info = context.getPackageManager().getApplicationInfo(line, PackageManager.GET_META_DATA);
                        mUids.add(info.uid);
                        Logger.d("Prevent list item uid: " + info.uid);
                    } catch (Exception e)
                    {}
                }
            }
        });

        hookAllMethods(ClassNames.ALARM_MANAGER_SERVICE, "setImpl", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                int uid = (int) param.args[param.args.length - 1];
                if (mUids.contains(uid)) {
                    long windowMillis = (long) param.args[2];
                    int flags = (int) param.args[5];
                    //Logger.d("Alarm from " + uid + " " + windowMillis + " " + Integer.toBinaryString(flags));

                    boolean isSet = false;
                    if (windowMillis >= 0) {  // 0 is WINDOW_EXACT, >0 is no later in milliseconds.
                        windowMillis = WINDOW_HEURISTIC;
                        param.args[2] = windowMillis;
                        isSet = true;
                    }
                    if ((flags & FLAG_ALLOW_WHILE_IDLE) != 0) {
                        flags &= ~FLAG_ALLOW_WHILE_IDLE;
                        param.args[5] = flags;
                        isSet = true;
                    }

                    if (isSet) {
                        Logger.i("Alarm from " + uid + " is set to " + windowMillis + ", " + Integer.toBinaryString(flags));
                    }
                }
            }
        });
    }
}
