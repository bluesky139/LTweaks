package li.lingfeng.ltweaks.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import org.apache.commons.lang3.StringUtils;

import li.lingfeng.ltweaks.MyApplication;

/**
 * Created by lilingfeng on 2017/7/5.
 */

public class NetUtils {

    public static WifiInfo getWifiInfo() {
        Context context = MyApplication.instance().getApplicationContext();
        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        final WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        final String ssid = wifiInfo.getSSID();
        final String bssid = wifiInfo.getBSSID();
        if (wifiInfo != null && !StringUtils.isAnyEmpty(ssid, bssid)
                && !ssid.equals("<unknown ssid>") && !bssid.equals("<none>")
                && !bssid.equals("00:00:00:00:00:00")) {
            return wifiInfo;
        }
        return null;
    }
}
