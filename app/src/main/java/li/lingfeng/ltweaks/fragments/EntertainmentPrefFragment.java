package li.lingfeng.ltweaks.fragments;

import android.os.Bundle;

import li.lingfeng.ltweaks.R;

/**
 * Created by smallville on 2017/1/7.
 */

public class EntertainmentPrefFragment extends BasePrefFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_entertainment);
    }
}
