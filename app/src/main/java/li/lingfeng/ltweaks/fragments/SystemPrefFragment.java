package li.lingfeng.ltweaks.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.activities.ImageSearchActivity;
import li.lingfeng.ltweaks.activities.ListCheckActivity;
import li.lingfeng.ltweaks.activities.QrCodeActivity;
import li.lingfeng.ltweaks.activities.SelectableTextActivity;
import li.lingfeng.ltweaks.activities.SolidExplorerUrlReplacerSettings;
import li.lingfeng.ltweaks.activities.TrustAgentWifiSettings;
import li.lingfeng.ltweaks.fragments.base.Extra;
import li.lingfeng.ltweaks.fragments.sub.system.PreventListDataProvider;
import li.lingfeng.ltweaks.fragments.sub.system.ShareFilterDataProvider;
import li.lingfeng.ltweaks.fragments.sub.system.TextActionDataProvider;
import li.lingfeng.ltweaks.lib.PreferenceChange;
import li.lingfeng.ltweaks.lib.PreferenceClick;
import li.lingfeng.ltweaks.prefs.ActivityRequestCode;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ComponentUtils;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.PackageUtils;
import li.lingfeng.ltweaks.utils.PermissionUtils;

/**
 * Created by smallville on 2017/1/4.
 */

public class SystemPrefFragment extends BasePrefFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_system);

        uncheckPreferenceByDisabledComponent(R.string.key_text_selectable_text, SelectableTextActivity.class);
        uncheckPreferenceByDisabledComponent(R.string.key_system_share_qrcode_scan, QrCodeActivity.class);
        uncheckPreferenceByDisabledComponent(R.string.key_system_share_image_search, ImageSearchActivity.class);
    }

    @PreferenceChange(prefs = R.string.key_text_aide_open_youdao)
    private void uninstallOldApp(Preference preference, boolean enabled) {
        if (enabled) {
            PackageUtils.tryUninstallPackage("li.lingfeng.textaide.youdao", "Text Aide with Youdao", getActivity());
        }
    }

    @PreferenceChange(prefs = R.string.key_text_selectable_text)
    private void enableSelectableText(Preference preference, boolean enabled) {
        ComponentUtils.enableComponent(SelectableTextActivity.class, enabled);
    }

    @PreferenceClick(prefs = R.string.key_text_actions)
    private void manageTextActions(Preference preference) {
        ListCheckActivity.create(getActivity(), TextActionDataProvider.class);
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
        pending.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, pending);
        getActivity().sendBroadcast(intent);

        Toast.makeText(getActivity(), R.string.app_shortcut_is_created, Toast.LENGTH_SHORT).show();
    }

    @PreferenceChange(prefs = R.string.key_system_share_qrcode_scan)
    private void systemShareQrcodeScan(final SwitchPreference preference, boolean enabled) {
        if (enabled) {
            PermissionUtils.requestPermissions(getActivity(), new PermissionUtils.ResultCallback() {
                @Override
                public void onResult(boolean ok) {
                    if (ok) {
                        ComponentUtils.enableComponent(QrCodeActivity.class, true);
                    } else {
                        preference.setChecked(false);
                    }
                }
            }, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        } else {
            ComponentUtils.enableComponent(QrCodeActivity.class, false);
        }
    }

    @PreferenceChange(prefs = R.string.key_system_share_image_search)
    private void systemShareImageSearch(final SwitchPreference preference, boolean enabled) {
        if (enabled) {
            PermissionUtils.requestPermissions(getActivity(), new PermissionUtils.ResultCallback() {
                @Override
                public void onResult(boolean ok) {
                    if (ok) {
                        ComponentUtils.enableComponent(ImageSearchActivity.class, true);
                    } else {
                        preference.setChecked(false);
                    }
                }
            }, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        } else {
            ComponentUtils.enableComponent(ImageSearchActivity.class, false);
        }
    }

    @PreferenceClick(prefs = R.string.key_system_share_filter)
    private void systemShareFilter(Preference preference) {
        ListCheckActivity.create(getActivity(), ShareFilterDataProvider.class);
    }

    @PreferenceClick(prefs = R.string.key_prevent_running_set_list)
    private void setPreventList(Preference preference) {
        ListCheckActivity.create(getActivity(), PreventListDataProvider.class);
    }

    @PreferenceChange(prefs = R.string.key_shadowsocks_primary_dns, refreshAtStart = true)
    private void setShadowsocksPrimaryDns(EditTextPreference preference, String value, Extra extra) {
        String[] dnsArray = StringUtils.split(value, ',');
        StringBuilder summary = new StringBuilder(getString(R.string.pref_shadowsocks_primary_dns_summary));
        for (String dns : dnsArray) {
            dns = StringUtils.strip(dns, " ");
            summary.append("\n");
            summary.append(dns);
        }
        preference.setSummary(summary);
    }

    @PreferenceChange(prefs = R.string.key_quick_settings_tile_4g3g, refreshAtStart = true)
    private void tile4G3G(SwitchPreference preference, boolean enabled, Extra extra) {
        ListPreference pref4g = findListPreference(R.string.key_quick_settings_tile_4g);
        ListPreference pref3g = findListPreference(R.string.key_quick_settings_tile_3g);
        pref4g.setEnabled(enabled);
        pref3g.setEnabled(enabled);

        if (extra.refreshAtStart) {
            Logger.d("Try get network types.");
            try {
                Context context = getActivity().createPackageContext(PackageNames.ANDROID_SETTINGS, Context.CONTEXT_IGNORE_SECURITY | Context.CONTEXT_INCLUDE_CODE);
                Class cls = Class.forName(ClassNames.RADIO_INFO, true, context.getClassLoader());
                Field field = cls.getDeclaredField("mPreferredNetworkLabels");
                field.setAccessible(true);
                String[] types = (String[]) field.get(Modifier.isStatic(field.getModifiers()) ? null : cls.newInstance());
                setTypesForListPreference(types, pref4g);
                setTypesForListPreference(types, pref3g);
            } catch (Throwable e) {
                Logger.e("Failed to get network types, " + e);
                Logger.stackTrace(e);
                pref4g.setEnabled(false);
                pref3g.setEnabled(false);
                preference.setEnabled(false);
                preference.setChecked(false);
                preference.setSummary(R.string.not_supported);
            }
        }
    }

    private void setTypesForListPreference(String[] types, ListPreference listPreference) {
        listPreference.setEntries(types);
        String[] entryValues = new String[types.length];
        for (int i = 0; i < types.length; ++i) {
            entryValues[i] = String.valueOf(i);
        }
        listPreference.setEntryValues(entryValues);
        listPreference.setSummary("%s");
    }

    @PreferenceClick(prefs = R.string.key_trust_agent_wifi_aps)
    private void setSmartLockWifiList(Preference preference) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            KeyguardManager keyguardManager = (KeyguardManager) getActivity().getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager.isKeyguardSecure()) {
                Intent keyguardIntent = keyguardManager.createConfirmDeviceCredentialIntent(getString(R.string.pref_trust_agent_wifi), "");
                startActivityForResult(keyguardIntent, ActivityRequestCode.KEYGUARD);
            } else {
                Toast.makeText(getActivity(), R.string.secure_lock_screen_not_setup, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getActivity(), R.string.not_supported, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ActivityRequestCode.KEYGUARD) {
            if (resultCode == Activity.RESULT_OK) {
                Intent intent = new Intent(getActivity(), TrustAgentWifiSettings.class);
                getActivity().startActivity(intent);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @PreferenceClick(prefs = R.string.key_solid_explorer_url_replacers)
    private void setupSolidExplorerUrlReplacer(Preference preference) {
        startActivity(new Intent(getActivity(), SolidExplorerUrlReplacerSettings.class));
    }

    @PreferenceChange(prefs = R.string.key_lineage_os_live_display_time, refreshAtStart = true)
    private void customizeLineageOSLiveDisplayTime(SwitchPreference preference, boolean enabled) {
        enablePreference(R.string.key_lineage_os_live_display_time_sunrise, enabled);
        enablePreference(R.string.key_lineage_os_live_display_time_sunset, enabled);
    }

    @PreferenceChange(prefs = R.string.key_display_min_brightness, refreshAtStart = true)
    private boolean setMinBrightness(EditTextPreference preference, String intValue, Extra extra) {
        if (extra.refreshAtStart) {
            PowerManager powerManager = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
            try {
                int defaultMinBrightness = (int) PowerManager.class.getDeclaredMethod("getMinimumScreenBrightnessSetting").invoke(powerManager);
                preference.setDialogTitle(getString(R.string.pref_display_min_brightness_dialog_title, defaultMinBrightness));
            } catch (Throwable e) {
                Logger.e("Can't get default min brightness, " + e);
            }
        }
        if (intValue.isEmpty()) {
            preference.setSummary("");
            return true;
        }
        int value = Integer.parseInt(intValue);
        if (value > 0 && value < 255) {
            preference.setSummary(intValue);
            return true;
        } else {
            return false;
        }
    }
}
