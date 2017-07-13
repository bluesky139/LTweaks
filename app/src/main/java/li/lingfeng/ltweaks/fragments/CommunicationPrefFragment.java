package li.lingfeng.ltweaks.fragments;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.activities.ZhiHuActivity;
import li.lingfeng.ltweaks.lib.PreferenceChange;
import li.lingfeng.ltweaks.prefs.Prefs;
import li.lingfeng.ltweaks.utils.ComponentUtils;
import li.lingfeng.ltweaks.utils.Logger;

/**
 * Created by smallville on 2017/1/15.
 */

public class CommunicationPrefFragment extends BasePrefFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_communication);

        uncheckPreferenceByDisabledComponent(R.string.key_zhihu_finish, ZhiHuActivity.class);
    }

    @PreferenceChange(prefs = R.string.key_wechat_use_incoming_ringtone, refreshAtStart = true)
    private void enableWeChatIncomingRingtone(SwitchPreference preference, Boolean enabled) {
        if (enabled == null) {
            enabled = preference.isChecked();
        }
        Preference setRingtonePref = findPreference(R.string.key_wechat_set_incoming_ringtone);
        setRingtonePref.setEnabled(enabled);
    }

    @PreferenceChange(prefs = R.string.key_wechat_set_incoming_ringtone, refreshAtStart = true)
    private void setWeChatIncomingRingtone(RingtonePreference preference, String path) {
        if (path == null) {
            path = Prefs.instance().getString(R.string.key_wechat_set_incoming_ringtone, "");
        }
        if (path.equals("")) {
            preference.setSummary("");
        } else {
            Uri uri = Uri.parse(path);
            Ringtone ring = RingtoneManager.getRingtone(getActivity(), uri);
            preference.setSummary(ring.getTitle(getActivity()));
        }
    }

    @PreferenceChange(prefs = R.string.key_zhihu_finish)
    private void enableZhiHuFinish(Preference preference, boolean enabled) {
        ComponentUtils.enableComponent(ZhiHuActivity.class, enabled);
    }
}
