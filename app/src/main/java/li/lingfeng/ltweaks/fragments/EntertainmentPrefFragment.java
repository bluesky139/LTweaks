package li.lingfeng.ltweaks.fragments;

import android.os.Bundle;
import android.preference.Preference;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.activities.BilibiliActivity;
import li.lingfeng.ltweaks.activities.BilibiliCoverActivity;
import li.lingfeng.ltweaks.activities.DoubanMovieActivity;
import li.lingfeng.ltweaks.activities.ProcessTextActivity;
import li.lingfeng.ltweaks.lib.PreferenceChange;
import li.lingfeng.ltweaks.utils.ComponentUtils;
import li.lingfeng.ltweaks.utils.Logger;

/**
 * Created by smallville on 2017/1/7.
 */

public class EntertainmentPrefFragment extends BasePrefFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_entertainment);

        uncheckPreferenceByDisabledComponent(R.string.key_douban_movie_url, DoubanMovieActivity.class);
        uncheckPreferenceByDisabledComponent(R.string.key_douban_movie_search,
                ComponentUtils.getFullAliasName(ProcessTextActivity.class, "DoubanMovie"));
        uncheckPreferenceByDisabledComponent(R.string.key_bilibili_search,
                ComponentUtils.getFullAliasName(ProcessTextActivity.class, "Bilibili"));
        uncheckPreferenceByDisabledComponent(R.string.key_bilibili_open_link_in_app, BilibiliActivity.class);
        uncheckPreferenceByDisabledComponent(R.string.key_bilibili_get_cover, BilibiliCoverActivity.class);
    }

    @PreferenceChange(prefs = R.string.key_douban_movie_url)
    private void enableDoubanMovieUrl(Preference preference, boolean enabled) {
        ComponentUtils.enableComponent(DoubanMovieActivity.class, enabled);
    }

    @PreferenceChange(prefs = R.string.key_douban_movie_search)
    private void enableDoubanMovieSearch(Preference preference, boolean enabled) {
        ComponentUtils.enableComponent(ComponentUtils.getFullAliasName(ProcessTextActivity.class, "DoubanMovie"), enabled);
    }

    @PreferenceChange(prefs = R.string.key_bilibili_search)
    private void enableBilibiliSearch(Preference preference, boolean enabled) {
        ComponentUtils.enableComponent(ComponentUtils.getFullAliasName(ProcessTextActivity.class, "Bilibili"), enabled);
    }

    @PreferenceChange(prefs = R.string.key_bilibili_open_link_in_app)
    private void enableBilibiliOpenLinkInApp(Preference preference, boolean enabled) {
        ComponentUtils.enableComponent(BilibiliActivity.class, enabled);
    }

    @PreferenceChange(prefs = R.string.key_bilibili_get_cover)
    private void enableBilibiliCover(Preference preference, boolean enabled) {
        ComponentUtils.enableComponent(BilibiliCoverActivity.class, enabled);
    }
}
