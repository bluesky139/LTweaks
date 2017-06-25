package li.lingfeng.ltweaks.prefs;

import android.content.Intent;
import android.hardware.Camera;
import android.net.ConnectivityManager;
import android.net.Proxy;
import android.net.wifi.WifiManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import li.lingfeng.ltweaks.xposed.system.XposedShareFilter;

/**
 * Created by smallville on 2017/3/29.
 */

public class IntentActions {

    public static final String[] sReceiverPreventedArray = new String[] {
            ConnectivityManager.CONNECTIVITY_ACTION,
            ConnectivityManager.ACTION_CAPTIVE_PORTAL_SIGN_IN,
            WifiManager.WIFI_STATE_CHANGED_ACTION,
            WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION,
            WifiManager.SUPPLICANT_STATE_CHANGED_ACTION,
            WifiManager.SCAN_RESULTS_AVAILABLE_ACTION,
            WifiManager.RSSI_CHANGED_ACTION,
            WifiManager.NETWORK_STATE_CHANGED_ACTION,
            WifiManager.NETWORK_IDS_CHANGED_ACTION,
            Proxy.PROXY_CHANGE_ACTION,
            Camera.ACTION_NEW_PICTURE,
            Camera.ACTION_NEW_VIDEO,
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_USER_UNLOCKED,
            Intent.ACTION_PACKAGES_SUSPENDED,
            Intent.ACTION_PACKAGES_UNSUSPENDED,
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_CHANGED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_RESTARTED,
            Intent.ACTION_PACKAGE_DATA_CLEARED,
            Intent.ACTION_PACKAGE_FIRST_LAUNCH,
            Intent.ACTION_PACKAGE_FULLY_REMOVED,
            Intent.ACTION_PACKAGE_NEEDS_VERIFICATION,
            Intent.ACTION_PACKAGE_VERIFIED,
            "android.intent.action.MY_PACKAGE_REPLACED",
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED,
            Intent.ACTION_SCREEN_OFF,
            Intent.ACTION_SCREEN_ON,
            Intent.ACTION_BATTERY_CHANGED,
            Intent.ACTION_REBOOT,
            Intent.ACTION_SHUTDOWN
    };
    public static final Set<String> sReceiverPrevented = new HashSet<>(Arrays.asList(sReceiverPreventedArray));

    public static final String[] sSendActionArray = {
            Intent.ACTION_SEND,
            Intent.ACTION_SEND_MULTIPLE
    };
    public static final Set<String> sSendActions = new HashSet<>(Arrays.asList(sSendActionArray));
}
