package li.lingfeng.ltweaks.xposed.communication;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/9/7.
 */
@XposedLoad(packages = {
        PackageNames.QQ_LITE,
        PackageNames.QQ,
        PackageNames.QQ_INTERNATIONAL,
        PackageNames.TIM
}, prefs = R.string.key_qq_disable_discussion_notification)
public class XposedQQDisableDiscussionNotification extends XposedBase {

    private static final String PROCESSOR_OBSERVER = "com.tencent.mobileqq.app.message.ProcessorObserver";
    private static final String MESSAGE_HANDLER = "com.tencent.mobileqq.app.MessageHandler";

    @Override
    protected void handleLoadPackage() throws Throwable {
        Class clsProcessorObserver = findClass(PROCESSOR_OBSERVER);
        Class[] parameterTypes = new Class[] {
                String.class, String.class, boolean.class, int.class, boolean.class, boolean.class
        };
        Method[] methods = XposedHelpers.findMethodsByExactParameters(clsProcessorObserver, void.class, parameterTypes);
        if (methods.length == 0) {
            throw new Exception("Can't find method from PROCESSOR_OBSERVER.");
        }

        Class clsMessageHandler = findClass(MESSAGE_HANDLER);
        if (!clsProcessorObserver.isAssignableFrom(clsMessageHandler)) {
            throw new Exception("clsProcessorObserver is not implemented by clsMessageHandler.");
        }
        Method method = XposedHelpers.findMethodExact(clsMessageHandler, methods[0].getName(), parameterTypes);
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if ("DiscMessageProcessor".equals(param.args[0]) && (boolean) param.args[4]) {
                    Logger.i("DiscMessageProcessor notification true -> false.");
                    param.args[4] = false;
                }
            }
        });
    }
}
