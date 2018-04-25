package li.lingfeng.ltweaks.utils;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by lilingfeng on 2017/8/29.
 */

public class XposedUtils {

    public static void unhookAll(Iterable<XC_MethodHook.Unhook> hooks) {
        for (XC_MethodHook.Unhook hook : hooks) {
            hook.unhook();
        }
    }

    public static Object getSurroundingThis(Object obj) throws Throwable {
        String name = obj.getClass().getName();
        int pos = name.lastIndexOf('$');
        if (pos > 0) {
            name = name.substring(0, pos);
            Class cls = XposedHelpers.findClass(name, obj.getClass().getClassLoader());
            Field field = XposedHelpers.findFirstFieldByExactType(obj.getClass(), cls);
            return field.get(obj);
        }
        return null;
    }
}
