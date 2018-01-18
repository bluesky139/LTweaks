package li.lingfeng.ltweaks.fragments;

import android.os.Bundle;
import android.preference.Preference;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.fragments.sub.donate.AlipayDonate;
import li.lingfeng.ltweaks.fragments.sub.donate.WeChatDonate;
import li.lingfeng.ltweaks.lib.PreferenceClick;

/**
 * Created by lilingfeng on 2018/1/18.
 */

public class DonateFragment extends BasePrefFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_donate);
    }

    @PreferenceClick(prefs = R.string.key_donate_alipay)
    private void donateWithAlipay(Preference preference) {
        AlipayDonate.donate(getActivity());
    }

    @PreferenceClick(prefs = R.string.key_donate_wechat)
    private void donateWithWeChat(Preference preference) {
        WeChatDonate.donate(getActivity());
    }
}
