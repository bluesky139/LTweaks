package li.lingfeng.ltweaks.xposed.system;

import android.content.Context;
import android.content.res.Resources;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
        findAndHookMethod("com.github.shadowsocks.ShadowsocksVpnService", "startShadowsocksDaemon", new XC_MethodHook() {
            private Set<Unhook> hooks;

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.d("startShadowsocksDaemon before");
                if (getProfileAcl(param.thisObject).equals("bypass-lan-china")) {
                    hooks = hookAllMethods("scala.collection.mutable.ArrayBuffer", "$plus$eq", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            String cmdParam = param.args[0].toString();
                            if (cmdParam.equals("--acl")) {
                                param.setResult(param.thisObject);
                            } else if (cmdParam.endsWith(".acl")) {
                                if (cmdParam.endsWith("/bypass-lan-china.acl")) {
                                    param.setResult(param.thisObject);
                                    Logger.i("--acl " + cmdParam + " is removed.");
                                } else {
                                    cmdParam = "--acl " + cmdParam;
                                    param.args[0] = cmdParam;
                                    Logger.i(cmdParam);
                                }
                            }
                        }
                    });
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Logger.d("startShadowsocksDaemon after");
                if (hooks != null) {
                    for (Unhook hook : hooks) {
                        hook.unhook();
                    }
                    hooks = null;
                }
            }
        });

        findAndHookMethod("com.github.shadowsocks.ShadowsocksVpnService", "startVpn", new XC_MethodHook() {
            private Unhook hook;

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.d("startVpn before");
                if (getProfileAcl(param.thisObject).equals("bypass-lan-china")) {
                    hook = findAndHookMethod(Resources.class, "getStringArray", int.class, new XC_MethodHook() {
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
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Logger.d("startVpn after");
                if (hook != null) {
                    hook.unhook();
                    hook = null;
                }
            }
        });
    }

    private String getProfileAcl(Object vpnService) throws Throwable {
        Method methodProfile = vpnService.getClass().getDeclaredMethod("profile");
        methodProfile.setAccessible(true);
        Object profile = methodProfile.invoke(vpnService);

        Method methodRoute = profile.getClass().getDeclaredMethod("route");
        methodRoute.setAccessible(true);
        return (String) methodRoute.invoke(profile);
    }
}
