package li.lingfeng.ltweaks.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.activities.QrCodeActivity;
import li.lingfeng.ltweaks.lib.PreferenceChange;
import li.lingfeng.ltweaks.lib.PreferenceClick;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ComponentUtils;
import li.lingfeng.ltweaks.utils.ContextUtils;
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

    @PreferenceClick(prefs = R.string.key_youdao_quick_query_shortcut)
    private void youdaoQuckQueryShortcut(Preference preference) {
        Context context = ContextUtils.createPackageContext(PackageNames.YOUDAO_DICT);
        Intent intent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        intent.putExtra("duplicate", false);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, ContextUtils.getString("app_name", context));
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context,
                ContextUtils.getDrawableId("logo_dict_large", context)));

        Intent pending = new Intent();
        pending.setClassName(PackageNames.YOUDAO_DICT, "com.youdao.dict.activity.QuickDictQueryActivity");
        pending.putExtra("isEditable", true);
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, pending);
        getActivity().sendBroadcast(intent);

        Toast.makeText(getActivity(), R.string.app_shortcut_is_created, Toast.LENGTH_SHORT).show();
    }

    @PreferenceChange(prefs = R.string.key_system_share_qrcode_scan)
    private void systemShareQrcodeScan(Preference preference, boolean enabled) {
        ComponentUtils.enableComponent(QrCodeActivity.class, enabled);
    }
}
