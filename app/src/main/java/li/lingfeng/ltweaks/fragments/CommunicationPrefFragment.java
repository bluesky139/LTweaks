package li.lingfeng.ltweaks.fragments;

import android.app.Activity;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.widget.Toast;

import java.io.File;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.PreferenceChange;
import li.lingfeng.ltweaks.lib.PreferenceClick;
import li.lingfeng.ltweaks.prefs.ActivityRequestCode;
import li.lingfeng.ltweaks.prefs.Prefs;
import li.lingfeng.ltweaks.utils.ComponentUtils;
import li.lingfeng.ltweaks.utils.IOUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.PermissionUtils;

/**
 * Created by smallville on 2017/1/15.
 */

public class CommunicationPrefFragment extends BasePrefFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_communication);
    }

    @PreferenceChange(prefs = R.string.key_wechat_use_incoming_ringtone, refreshAtStart = true)
    private void enableWeChatIncomingRingtone(SwitchPreference preference, boolean enabled) {
        Preference setRingtonePref = findPreference(R.string.key_wechat_set_incoming_ringtone);
        setRingtonePref.setEnabled(enabled);
    }

    @PreferenceChange(prefs = R.string.key_wechat_set_incoming_ringtone, refreshAtStart = true)
    private void setWeChatIncomingRingtone(RingtonePreference preference, String path) {
        if (path.equals("")) {
            preference.setSummary("");
        } else {
            Uri uri = Uri.parse(path);
            Ringtone ring = RingtoneManager.getRingtone(getActivity(), uri);
            preference.setSummary(ring.getTitle(getActivity()));
        }
    }

    @PreferenceChange(prefs = R.string.key_qq_clear_background, refreshAtStart = true)
    private void enableQQClearBackground(SwitchPreference preference, boolean enabled) {
        findPreference(R.string.key_qq_clear_background_path).setEnabled(enabled);
    }

    @PreferenceClick(prefs = R.string.key_qq_clear_background_path)
    private void setQQClearBackgroundPath(Preference preference) {
        PermissionUtils.requestPermissions(getActivity(), new PermissionUtils.ResultCallback() {
            @Override
            public void onResult(boolean ok) {
                if (ok) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivityForResult(intent, ActivityRequestCode.QQ_CLEAR_IMAGE_CHOOSER);
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ActivityRequestCode.QQ_CLEAR_IMAGE_CHOOSER) {
            Preference preference = findPreference(R.string.key_qq_clear_background_path);
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = data.getData();
                String filepath = getQQClearBackgroundPath();
                if (filepath == null) {
                    Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (IOUtils.saveUriToFile(uri, filepath)) {
                    Logger.i("New qq background is set.");
                    preference.setSummary(getString(R.string.pref_qq_clear_background_path_summary, filepath));
                } else {
                    Logger.e("New qq background set error.");
                    preference.setSummary(R.string.error);
                }
            } else {
                Logger.i("New qq background is not selected.");
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private String getQQClearBackgroundPath() {
        File dir = Environment.getExternalStoragePublicDirectory("Tencent");
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Logger.e("Can't create dir " + dir.getAbsolutePath());
                return null;
            }
        }
        return dir.getAbsolutePath() + "/ltweaks_qq_background";
    }

    @PreferenceChange(prefs = R.string.key_qq_clear_background_path, refreshAtStart = true)
    private void refreshQQClearBackgroundPathSummary(Preference preference) {
        String filepath = getQQClearBackgroundPath();
        if (filepath == null) {
            return;
        }
        File file = new File(filepath);
        if (file.exists()) {
            preference.setSummary(getString(R.string.pref_qq_clear_background_path_summary, filepath));
        }
    }
}
