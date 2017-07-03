package li.lingfeng.ltweaks.fragments;

import android.os.Bundle;
import android.preference.Preference;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.activities.ProcessTextActivity;
import li.lingfeng.ltweaks.lib.PreferenceChange;
import li.lingfeng.ltweaks.utils.ComponentUtils;

/**
 * Created by smallville on 2017/1/7.
 */

public class EntertainmentPrefFragment extends BasePrefFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_entertainment);
    }

    @PreferenceChange(prefs = R.string.key_douban_movie_search)
    private void enableDoubanMovieSearch(Preference preference, boolean enabled) {
        ComponentUtils.enableComponent(ComponentUtils.getFullAliasName(ProcessTextActivity.class, "DoubanMovie"), enabled);
    }

    @PreferenceChange(prefs = R.string.key_bilibili_search)
    private void enableBilibiliSearch(Preference preference, boolean enabled) {
        ComponentUtils.enableComponent(ComponentUtils.getFullAliasName(ProcessTextActivity.class, "Bilibili"), enabled);
    }
}
