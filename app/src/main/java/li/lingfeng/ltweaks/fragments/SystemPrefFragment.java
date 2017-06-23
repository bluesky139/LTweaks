package li.lingfeng.ltweaks.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.widget.Toast;

import java.util.List;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.activities.ImageSearchActivity;
import li.lingfeng.ltweaks.activities.ListCheckActivity;
import li.lingfeng.ltweaks.activities.QrCodeActivity;
import li.lingfeng.ltweaks.lib.PreferenceChange;
import li.lingfeng.ltweaks.lib.PreferenceClick;
import li.lingfeng.ltweaks.lib.PreferenceLongClick;
import li.lingfeng.ltweaks.prefs.IntentActions;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ComponentUtils;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.PermissionUtils;
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
        ListCheckActivity.create(getActivity(), DataProvider.class);
    }

    public static class DataProvider extends ListCheckActivity.DataProvider {

        private List<ResolveInfo> mAppInfos;

        public DataProvider(Activity activity) {
            super(activity);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("*/*");
            mAppInfos = activity.getPackageManager().queryIntentActivities(intent, 0);
        }

        @Override
        protected String[] getTabTitles() {
            return new String[] { "All", "Disabled", "Enabled" };
        }

        @Override
        protected int getListItemCount(int tab) {
            return mAppInfos.size();
        }

        @Override
        protected ListItem getListItem(int tab, int position) {
            ListItem item = new ListItem();
            ResolveInfo info = mAppInfos.get(position);
            item.mIcon = info.loadIcon(mActivity.getPackageManager());
            item.mTitle = info.activityInfo.applicationInfo.loadLabel(mActivity.getPackageManager());
            item.mDescription = info.loadLabel(mActivity.getPackageManager());
            return item;
        }
    }

    @PreferenceLongClick(prefs = R.string.key_prevent_running_prevent_receiver)
    private void seeReceiverPreventedActions(final SwitchPreference preference) {
        new AlertDialog.Builder(getActivity())
                .setTitle("Actions")
                .setItems(IntentActions.sReceiverPreventedArray, null)
                .create()
                .show();
    }
}
