package li.lingfeng.ltweaks.fragments;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.activities.JDActivity;
import li.lingfeng.ltweaks.activities.JDHistoryActivity;
import li.lingfeng.ltweaks.lib.PreferenceChange;

/**
 * Created by smallville on 2016/12/25.
 */

public class ShoppingPrefFragment extends BasePrefFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_shopping);
    }

    @PreferenceChange(prefs = R.string.key_jd_open_link_in_app)
    private void enableJdOpenLinkInApp(Preference preference, boolean enabled) {
        ComponentName componentName = new ComponentName(getActivity(), JDActivity.class);
        getActivity().getPackageManager().setComponentEnabledSetting(componentName,
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    @PreferenceChange(prefs = R.string.key_jd_history)
    private void enableJdHistory(Preference preference, boolean enabled) {
        ComponentName componentName = new ComponentName(getActivity(), JDHistoryActivity.class);
        getActivity().getPackageManager().setComponentEnabledSetting(componentName,
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
}
