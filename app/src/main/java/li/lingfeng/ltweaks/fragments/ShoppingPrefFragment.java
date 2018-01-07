package li.lingfeng.ltweaks.fragments;

import android.os.Bundle;
import android.preference.Preference;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.activities.JDHistoryActivity;
import li.lingfeng.ltweaks.lib.PreferenceChange;
import li.lingfeng.ltweaks.utils.ComponentUtils;

/**
 * Created by smallville on 2016/12/25.
 */

public class ShoppingPrefFragment extends BasePrefFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_shopping);

        uncheckPreferenceByDisabledComponent(R.string.key_jd_history, JDHistoryActivity.class);
        uncheckPreferenceByDisabledComponent(R.string.key_suning_history, JDHistoryActivity.class);
    }

    @PreferenceChange(prefs = { R.string.key_jd_history, R.string.key_suning_history })
    private void enableJdHistory(Preference preference, boolean enabled) {
        ComponentUtils.enableComponent(JDHistoryActivity.class, enabled);
        findSwitchPreference(R.string.key_jd_history).setChecked(enabled);
        findSwitchPreference(R.string.key_suning_history).setChecked(enabled);
    }
}
