package li.lingfeng.ltweaks.xposed.system;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.widget.Toast;

import java.util.List;

import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Callback;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.Shell;

/**
 * Created by lilingfeng on 2017/6/26.
 */
@XposedLoad(packages = PackageNames.ANDROID_SYSTEM_UI, prefs = R.string.key_quick_settings_tile_adb_wireless)
public class XposedAdbWireless extends XposedTile {

    @Override
    protected int getPriority() {
        return 0;
    }

    @Override
    protected String getTileName(boolean isOn) {
        if (!isOn) {
            try {
                WifiManager wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                int ip = wifiManager.getConnectionInfo().getIpAddress();
                String strIp = Formatter.formatIpAddress(ip);
                Logger.d("Got ip " + strIp);
                return strIp;
            } catch (Throwable e) {
                Logger.e("Can't get ip, " + e);
            }
        }
        return "Adb Wireless";
    }

    @Override
    protected String getTileDesc() {
        return "Use \"adb connect x:x:x:x:5555\" to connect.";
    }

    @Override
    protected int getTileIcon(boolean isOn) {
        return isOn ? R.drawable.ic_quick_settings_adb_wireless_off : R.drawable.ic_quick_settings_adb_wireless_on;
    }

    @Override
    protected void onSwitch(final Context context, final boolean isOn) throws Throwable {
        new Shell("su", new String[] {
                "setprop service.adb.tcp.port " + (isOn ? "5555" : "-1"),
                "stop adbd",
                "start adbd"
        },
                3000, new Callback.C3<Boolean, List<String>, List<String>>() {
            @Override
            public void onResult(Boolean isOk, List<String> stderr, List<String> stdout) {
                Logger.d("Adb Wireless onResult " + isOk);
                if (isOk) {
                    Toast.makeText(context, isOn ? "Switched to adb wireless" : "Switched to adb usb", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "Failed to switch adb, no root?", Toast.LENGTH_LONG).show();
                    updateTileState(isOn);
                }
            }
        }).execute();
    }

    @Override
    protected void onLongClick(Context context) throws Throwable {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setComponent(new ComponentName(PackageNames.ANDROID_SETTINGS, ClassNames.DEVELOPMENT_SETTINGS));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        collapseStatusBar();
    }

    @Override
    protected boolean enableNotification() {
        return true;
    }

    @Override
    protected int getNotificationId() {
        Class cls = findClass("com.android.internal.R.string");
        return XposedHelpers.getStaticIntField(cls, "adb_active_notification_message");
    }

    @Override
    protected int getNotificationIcon() {
        Class cls = findClass("com.android.internal.R.drawable");
        return XposedHelpers.getStaticIntField(cls, "stat_sys_adb");
    }

    @Override
    protected String getNotificationTitle() {
        return ContextUtils.getLString(R.string.adb_wireless_notification_title);
    }

    @Override
    protected String getNotificationText() {
        return ContextUtils.getLString(R.string.adb_wireless_notification_text);
    }
}
