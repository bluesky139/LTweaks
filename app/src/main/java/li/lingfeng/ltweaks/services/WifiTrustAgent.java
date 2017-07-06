package li.lingfeng.ltweaks.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.service.trust.TrustAgentService;

import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.prefs.Prefs;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.NetUtils;
import li.lingfeng.ltweaks.utils.Utils;

/**
 * Created by lilingfeng on 2017/7/6.
 */

public class WifiTrustAgent extends TrustAgentService implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Receiver mReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.d("WifiTrustAgent onCreate");
        setManagingTrust(true);
        Prefs.instance().registerOnSharedPreferenceChangeListener(this);

        mReceiver = new Receiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.setPriority(-1);
        registerReceiver(mReceiver, filter);
        updateTrust();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.d("WifiTrustAgent onDestroy");
        unregisterReceiver(mReceiver);
        Prefs.instance().unregisterOnSharedPreferenceChangeListener(this);
        revokeTrust();
    }

    private void updateTrust() {
        try {
            WifiInfo wifiInfo = NetUtils.getWifiInfo();
            if (wifiInfo != null) {
                String ssid = StringUtils.strip(wifiInfo.getSSID(), "\"");
                String bssid = wifiInfo.getBSSID();
                Set<String> aps = Prefs.instance().getStringSet(R.string.key_trust_agent_wifi_aps, new HashSet<String>());
                for (String ap : aps) {
                    String[] s = Utils.splitByLastChar(ap, ',');
                    if (s[0].equals(ssid) && s[1].equals(bssid)) {
                        Logger.i("grantTrust by " + ap);
                        grantTrust("LTweaks wifi trust", 0, 0);
                        return;
                    }
                }
            }
            Logger.i("revokeTrust by ap loss");
            revokeTrust();
        } catch (Throwable e) {
            Logger.e("Wifi trust grant or revoke error, " + e);
            Logger.stackTrace(e);
            revokeTrust();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (getString(R.string.key_trust_agent_wifi_aps).equals(key)) {
            updateTrust();
        }
    }

    private class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                return;
            }
            updateTrust();
        }
    }
}
