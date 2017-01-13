package li.lingfeng.ltweaks.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v7.app.AlertDialog;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.PreferenceChange;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.UninstallUtils;

/**
 * Created by smallville on 2017/1/4.
 */

public class SystemPrefFragment extends BasePrefFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_system);
    }

    @PreferenceChange(prefs = R.string.key_text_aide_open_youdao)
    private void uninstallOldApp(Preference preference, boolean enabled) {
        if (enabled) {
            UninstallUtils.tryUninstallPackage("li.lingfeng.textaide.youdao", "Text Aide with Youdao", getActivity());
        }
    }
}
