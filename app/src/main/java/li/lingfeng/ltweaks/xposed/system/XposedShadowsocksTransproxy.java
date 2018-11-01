package li.lingfeng.ltweaks.xposed.system;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.MyApplication;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Callback;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.PackageUtils;
import li.lingfeng.ltweaks.utils.Shell;
import li.lingfeng.ltweaks.xposed.XposedBase;

@XposedLoad(packages = PackageNames.SHADOWSOCKS, prefs = R.string.key_shadowsocks_transproxy)
public class XposedShadowsocksTransproxy extends XposedBase {

    private static final String TRANSPROXY_SERVICE = "com.github.shadowsocks.bg.TransproxyService";
    private static final String APP = "com.github.shadowsocks.App";
    private static final String BASE_SERVICE_DATA = "com.github.shadowsocks.bg.BaseService$Data";
    private static final String DATA_SOURCE = "com.github.shadowsocks.preference.DataStore";

    private Shell mShellAdding;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(TRANSPROXY_SERVICE, "startNativeProcesses", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                final long startTime = System.currentTimeMillis();
                String[] ipList = ContextUtils.getLStringArray(R.array.shadowsocks_bypass_ip_list);
                String[] preCmds = new String[] {
                        "iptables -w -t nat -D OUTPUT -j Shadowsocks",
                        "iptables -w -t nat -F Shadowsocks",
                        "iptables -w -t nat -N Shadowsocks",
                        "iptables -w -t nat -A Shadowsocks -o lo -j RETURN",
                        "iptables -w -t nat -A Shadowsocks -d 127.0.0.1 -j RETURN",
                        "iptables -w -t nat -A Shadowsocks -m owner --uid-owner " + PackageUtils.getUid() + " -j RETURN",
                        "iptables -w -t nat -A Shadowsocks -p udp --dport 53 -j DNAT --to-destination 127.0.0.1:5450",
                        "iptables -w -t nat -A Shadowsocks -p tcp --dport 53 -j DNAT --to-destination 127.0.0.1:5450",
                };
                String[] cmds = new String[preCmds.length + ipList.length + 2];
                System.arraycopy(preCmds, 0, cmds, 0, preCmds.length);
                for (int i = 0; i < ipList.length; ++i) {
                    cmds[i + preCmds.length] = "iptables -w -t nat -A Shadowsocks -p all -d " + ipList[i] + " -j RETURN";
                }
                cmds[cmds.length - 2] = "iptables -w -t nat -A Shadowsocks -p tcp -j DNAT --to-destination 127.0.0.1:8200";
                cmds[cmds.length - 1] = "iptables -w -t nat -A OUTPUT -j Shadowsocks";

                createOngoingNotification();
                synchronized (XposedShadowsocksTransproxy.this) {
                    mShellAdding = new Shell("su", cmds, 0, new Callback.C3<Boolean, List<String>, List<String>>() {
                        @Override
                        public void onResult(Boolean isOk, List<String> stderr, List<String> stdout) {
                            synchronized (XposedShadowsocksTransproxy.this) {
                                mShellAdding = null;
                            }
                            isOk = isOk & (stderr.size() < 20);
                            Logger.d("XposedShadowsocksTransproxy start result " + isOk + ", cost " + (System.currentTimeMillis() - startTime) + "ms");
                            toast("iptables set " + isOk);
                            cancelOngoingNotification();
                            if (!isOk) {
                                createFailureNotification();
                            }
                        }
                    });
                    mShellAdding.execute();
                }
            }
        });

        findAndHookMethod(TRANSPROXY_SERVICE, "stopRunner", boolean.class, String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                synchronized (XposedShadowsocksTransproxy.this) {
                    if (mShellAdding != null) {
                        mShellAdding.forceClean();
                        mShellAdding = null;
                    }
                }

                String[] cmds = new String[] {
                        "iptables -w -t nat -D OUTPUT -j Shadowsocks",
                        "iptables -w -t nat -F Shadowsocks",
                        "iptables -w -t nat -X Shadowsocks",
                };
                new Shell("su", cmds, 0, new Callback.C3<Boolean, List<String>, List<String>>() {
                    @Override
                    public void onResult(Boolean isOk, List<String> stderr, List<String> stdout) {
                        isOk = isOk & (stderr.size() == 0);
                        Logger.d("XposedShadowsocksTransproxy stop result " + isOk);
                        toast("iptables unset " + isOk);
                    }
                }).execute();
            }
        });

        findAndHookMethod(BASE_SERVICE_DATA, "getAclFile", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object dataSource = XposedHelpers.getStaticObjectField(findClass(DATA_SOURCE), "INSTANCE");
                String serviceMode = (String) XposedHelpers.callMethod(dataSource, "getServiceMode");
                if (serviceMode.equals("transproxy")) {
                    Logger.d("Remove ss-local acl on transproxy mode.");
                    param.setResult(null);
                }
            }
        });
    }

    private void createOngoingNotification() {
        cancelNotification(8201);
        createNotification(8200, true, "Adding iptables rules...");
    }

    private void cancelOngoingNotification() {
        cancelNotification(8200);
    }

    private void createFailureNotification() {
        createNotification(8201, false, "Failed to add iptables rules.");
    }

    private void createNotification(int id, boolean isOngoing, String text) {
        NotificationManager notificationManager = (NotificationManager) MyApplication.instance().getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(MyApplication.instance())
                .setSmallIcon(ContextUtils.getDrawableId("ic_service_active"))
                .setWhen(0)
                .setOngoing(isOngoing)
                .setTicker(text)
                .setDefaults(0)
                .setPriority(Notification.PRIORITY_LOW)
                .setContentTitle("Shadowsocks transproxy")
                .setContentText(text)
                .setChannelId("Shadowsocks transproxy iptables");
        if (isOngoing) {
            builder.setProgress(0, 0, true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                    new NotificationChannel("Shadowsocks transproxy iptables", "Shadowsocks transproxy iptables", NotificationManager.IMPORTANCE_LOW));
        }
        Notification notification = builder.build();
        notificationManager.notify(id, notification);
    }

    private void cancelNotification(int id) {
        NotificationManager notificationManager = (NotificationManager) MyApplication.instance().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(id);
    }

    private void toast(final String msg) {
        final Application app = (Application) XposedHelpers.getStaticObjectField(findClass(APP), "app");
        Handler handler = (Handler) XposedHelpers.callMethod(app, "getHandler");
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(app, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
