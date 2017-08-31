package li.lingfeng.ltweaks.utils;

import de.robv.android.xposed.XC_MethodHook;

/**
 * Created by lilingfeng on 2017/8/29.
 */

public class XposedUtils {

    public static void unhookAll(Iterable<XC_MethodHook.Unhook> hooks) {
        for (XC_MethodHook.Unhook hook : hooks) {
            hook.unhook();
        }
    }
}
