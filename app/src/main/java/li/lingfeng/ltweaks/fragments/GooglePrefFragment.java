package li.lingfeng.ltweaks.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.support.v7.app.AlertDialog;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.PreferenceChange;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.UninstallUtils;

/**
 * Created by smallville on 2016/12/24.
 */

public class GooglePrefFragment extends BasePrefFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_google);
    }

    @PreferenceChange(prefs = {
            R.string.key_google_plus_remove_bottom_bar,
            R.string.key_google_photos_remove_bottom_bar,
            R.string.key_google_play_view_in_coolapk
    })
    private void uninstallOldApp(Preference preference, boolean enabled) {
        if (!enabled) {
            return;
        }

        String packageName = "";
        String appName = "";
        if (preference.getKey().equals(getString(R.string.key_google_plus_remove_bottom_bar))) {
            packageName = "li.lingfeng.google.plus.bottombarremover";
            appName = "Google Plus Bottom Bar Remover";
        } else if (preference.getKey().equals(getString(R.string.key_google_photos_remove_bottom_bar))) {
            packageName = "li.lingfeng.google.photos.bottombarremover";
            appName = "Google Photos Bottom Bar Remover";
        } else if (preference.getKey().equals(getString(R.string.key_google_play_view_in_coolapk))) {
            packageName = "li.lingfeng.viewincoolapk";
            appName = "View in Coolapk";
        } else {
            Logger.e("Unknown pref for app uninstall, " + preference.getKey());
            return;
        }
        UninstallUtils.tryUninstallPackage(packageName, appName, getActivity());
    }

    @PreferenceChange(prefs = R.string.key_google_plus_remove_bottom_bar)
    private void setGooglePlusNewPostsPosition(Preference preference, boolean enabled) {
        SwitchPreference newPostsPreference = findSwitchPreference(R.string.key_google_plus_top_right_refresh);
        if (!enabled) {
            newPostsPreference.setChecked(false);
            newPostsPreference.setEnabled(false);
        } else {
            newPostsPreference.setEnabled(true);
        }
    }
}
