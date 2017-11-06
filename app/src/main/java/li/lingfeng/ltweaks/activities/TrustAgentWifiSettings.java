package li.lingfeng.ltweaks.activities;

import android.app.Activity;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.widget.CompoundButton;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.prefs.Prefs;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.NetUtils;
import li.lingfeng.ltweaks.utils.Utils;

/**
 * Created by lilingfeng on 2017/7/6.
 */

public class TrustAgentWifiSettings extends ListCheckActivity {

    @Override
    protected Class<? extends ListCheckActivity.DataProvider> getDataProviderClass() {
        return DataProvider.class;
    }

    public static class DataProvider extends ListCheckActivity.DataProvider {

        private Set<String> mTrustedAps;
        private List<ListItem> mListItems;

        public DataProvider(ListCheckActivity activity) {
            super(activity);
            mTrustedAps = new HashSet<>(
                    Prefs.instance().getStringSet(R.string.key_trust_agent_wifi_aps, new HashSet<String>())
            );
            mListItems = new ArrayList<>(mTrustedAps.size() + 1);
            WifiInfo wifiInfo = NetUtils.getWifiInfo();
            boolean hasCurrent = false;
            for (String ap : mTrustedAps) {
                final String[] s = Utils.splitByLastChar(ap, ',');
                boolean isCurrent = (wifiInfo != null && StringUtils.strip(wifiInfo.getSSID(), "\"").equals(s[0]) && wifiInfo.getBSSID().equals(s[1]));
                hasCurrent |= isCurrent;
                ListItem item = createListItem(s[0], s[1], isCurrent, true);
                mListItems.add(item);
            }
            if (wifiInfo != null && !hasCurrent) {
                ListItem item = createListItem(StringUtils.strip(wifiInfo.getSSID(), "\""), wifiInfo.getBSSID(), true, false);
                mListItems.add(item);
            }
        }

        private ListItem createListItem(final String ssid, final String bssid, boolean isCurrent, boolean isChecked) {
            ListItem item = new ListItem();
            item.mData = new Pair<>(ssid, bssid);
            item.mIcon = mActivity.getResources().getDrawable(R.drawable.ic_wifi);
            item.mTitle = ssid + (isCurrent ? (" (" + mActivity.getString(R.string.current) + ")") : "");
            item.mDescription = bssid;
            item.mChecked = isChecked;
            return item;
        }

        @Override
        protected String getActivityTitle() {
            return mActivity.getString(R.string.pref_trust_agent_wifi);
        }

        @Override
        protected String[] getTabTitles() {
            return new String[] { mActivity.getString(R.string.list) };
        }

        @Override
        protected int getListItemCount(int tab) {
            return mListItems.size();
        }

        @Override
        protected ListItem getListItem(int tab, int position) {
            return mListItems.get(position);
        }

        @Override
        protected boolean reload() {
            return false;
        }

        @Override
        public void onCheckedChanged(ListItem item, Boolean isChecked) {
            Pair pair = (Pair) item.mData;
            String ap = pair.first + "," + pair.second;
            if (isChecked) {
                Logger.i("Trust wifi " + ap);
                mTrustedAps.add(ap);
            } else {
                Logger.i("Revoke wifi " + ap);
                mTrustedAps.remove(ap);
            }
            Prefs.instance().edit()
                    .putStringSet(R.string.key_trust_agent_wifi_aps, mTrustedAps)
                    .commit();
        }
    }
}
