package li.lingfeng.ltweaks.xposed.system;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.format.Formatter;
import android.widget.Toast;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Callback;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.Shell;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/6/26.
 */
@XposedLoad(packages = PackageNames.ANDROID_SYSTEM_UI, prefs = R.string.key_quick_settings_tile_adb_wireless)
public class XposedAdbWireless extends XposedBase {

    private static final String ACTION_UPDATE_STATE = XposedAdbWireless.class.getName() + ".ACTION_UPDATE_STATE";
    private static final String ACTION_ADB_SWITCH = XposedAdbWireless.class.getName() + ".ACTION_ADB_SWITCH";
    private Context mContext;
    private AdbSwitchReceiver mReceiver;

    @Override
    protected void handleLoadPackage() throws Throwable {
        final Class clsQsTileHost = findClass(ClassNames.QS_TILE_HOST);
        final Class clsIntentTile = findClass(ClassNames.INTENT_TILE);

        String methodOnTuningChanged = "onTuningChanged";
        try {
            XposedHelpers.findMethodExact(clsQsTileHost, "onTuningChanged", String.class, String.class);
        } catch (Throwable e) {
            methodOnTuningChanged = "recreateTiles";
        }

        hookAllMethods(clsQsTileHost, methodOnTuningChanged, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                if (mReceiver == null) {
                    Logger.i("Register adb switch receiver.");
                    mReceiver = new AdbSwitchReceiver();
                    IntentFilter filter = new IntentFilter();
                    filter.addAction(ACTION_ADB_SWITCH);
                    mContext.registerReceiver(mReceiver, filter);
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                updateTileState(true);
            }
        });

        hookAllMethods(clsQsTileHost, "loadTileSpecs", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("loadTileSpecs return one more tile adb_wireless");
                List<String> tiles = (List<String>) param.getResult();
                tiles.add("adb_wireless");
            }
        });

        findAndHookMethod(clsQsTileHost, "createTile", String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String tileSpec = (String) param.args[0];
                if (tileSpec.equals("adb_wireless")) {
                    Logger.i("Create adb_wireless tile.");
                    Object tile = XposedHelpers.callStaticMethod(clsIntentTile, "create", param.thisObject, "intent(" + ACTION_UPDATE_STATE + ")");
                    param.setResult(tile);
                }
            }
        });
    }

    private class AdbSwitchReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, Intent intent) {
            if (!intent.getAction().equals(ACTION_ADB_SWITCH)) {
                return;
            }

            final boolean isWireless = intent.getBooleanExtra("is_wireless", true);
            new Shell("su", new String[] {
                        "setprop service.adb.tcp.port " + (isWireless ? "5555" : "-1"),
                        "stop adbd",
                        "start adbd"
                    },
                    3000, new Callback.C3<Boolean, List<String>, List<String>>() {
                @Override
                public void onResult(Boolean isOk, List<String> stderr, List<String> stdout) {
                    Logger.d("Adb Wireless onResult " + isOk);
                    if (isOk) {
                        Toast.makeText(context, isWireless ? "Switched to adb wireless" : "Switched to adb usb", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Failed to switch adb, no root?", Toast.LENGTH_SHORT).show();
                        updateTileState(isWireless);
                    }
                }
            }).execute();
            updateTileState(!isWireless);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void updateTileState(boolean isWireless) {
        Logger.i("AdbWireless updateTileState");
        Intent intent = new Intent(ACTION_UPDATE_STATE);
        intent.putExtra("visible", true);
        intent.putExtra("contentDescription", "Use \"adb connect x:x:x:x:5555\" to connect.");
        intent.putExtra("label", "Adb Wireless");
        intent.putExtra("iconPackage", PackageNames.L_TWEAKS);
        if (isWireless) {
            intent.putExtra("iconId", R.drawable.ic_quick_settings_adb_wireless_off);
        } else {
            intent.putExtra("iconId", R.drawable.ic_quick_settings_adb_wireless_on);
            try {
                WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                int ip = wifiManager.getConnectionInfo().getIpAddress();
                String strIp = Formatter.formatIpAddress(ip);
                intent.putExtra("label", strIp);
                Logger.d("Got ip " + strIp);
            } catch (Exception e) {
                Logger.e("Can't get ip, " + e);
            }
        }

        Intent clickIntent = new Intent(ACTION_ADB_SWITCH);
        clickIntent.putExtra("is_wireless", isWireless);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        intent.putExtra("onClick", pendingIntent);
        mContext.sendBroadcast(intent);

        try {
            NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            Class cls = findClass("com.android.internal.R.string");
            int id = XposedHelpers.getStaticIntField(cls, "adb_active_notification_message");
            if (!isWireless) {
                cls = findClass("com.android.internal.R.drawable");
                Notification.Builder builder = new Notification.Builder(mContext)
                        .setSmallIcon(XposedHelpers.getStaticIntField(cls, "stat_sys_adb"))
                        .setWhen(0)
                        .setOngoing(true)
                        .setTicker(ContextUtils.getLString(R.string.adb_wireless_notification_title))
                        .setDefaults(0)
                        .setPriority(Notification.PRIORITY_LOW)
                        .setContentTitle(ContextUtils.getLString(R.string.adb_wireless_notification_title))
                        .setContentText(ContextUtils.getLString(R.string.adb_wireless_notification_text))
                        .setContentIntent(pendingIntent);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    builder.setVisibility(Notification.VISIBILITY_PUBLIC);
                }
                Notification notification = builder.build();
                notificationManager.notify(id, notification);
            } else {
                notificationManager.cancel(id /* com.android.internal.R.string.adb_active_notification_message */);
            }
        } catch (Throwable e) {
            Logger.e("Can't set notification for adb wireless, " + e);
        }
    }
}
