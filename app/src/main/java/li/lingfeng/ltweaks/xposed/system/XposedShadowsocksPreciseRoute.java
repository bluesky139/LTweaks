package li.lingfeng.ltweaks.xposed.system;

import android.content.Context;
import android.content.res.Resources;

import java.util.Arrays;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.MyApplication;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/5/30.
 */
@XposedLoad(packages = PackageNames.SHADOWSOCKS, prefs = R.string.key_shadowsocks_precise_route)
public class XposedShadowsocksPreciseRoute extends XposedBase {

    @Override
    protected Class getApplicationClass() {
        return findClass("com.github.shadowsocks.ShadowsocksApplication");
    }

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(Resources.class, "getStringArray", int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                final int bypassPrivateRouteId = ContextUtils.getResId("bypass_private_route", "array");
                if (bypassPrivateRouteId == (int) param.args[0]) {
                    Context context = MyApplication.instance().createPackageContext(PackageNames.L_TWEAKS, 0);
                    String[] routes = context.getResources().getStringArray(R.array.shadowsocks_bypass_private_route);
                    param.setResult(routes);
                    Logger.i("Return precise bypass_private_route, count " + routes.length);
                }
            }
        });

        findAndHookMethod("com.github.shadowsocks.ShadowsocksVpnService", "startShadowsocksDaemon", new XC_MethodHook() {
            private Set<Unhook> hooks;

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.d("startShadowsocksDaemon before");
                hooks = hookAllMethods("scala.collection.mutable.ArrayBuffer", "$plus$eq", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String cmdParam = param.args[0].toString();
                        if (cmdParam.equals("--acl")) {
                            param.setResult(param.thisObject);
                        } else if (cmdParam.endsWith(".acl")) {
                            if (cmdParam.endsWith("/bypass-lan-china.acl")) {
                                param.setResult(param.thisObject);
                                Logger.d("--acl " + cmdParam + " is removed.");
                            } else {
                                cmdParam = "--acl " + cmdParam;
                                param.args[0] = cmdParam;
                                Logger.d(cmdParam);
                            }
                        }
                    }
                });
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Logger.d("startShadowsocksDaemon after");
                for (Unhook hook : hooks) {
                    hook.unhook();
                }
                hooks = null;
            }
        });
    }
}
