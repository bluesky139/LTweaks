package li.lingfeng.ltweaks.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.prefs.Prefs;
import li.lingfeng.ltweaks.utils.Logger;

/**
 * Created by lilingfeng on 2017/7/7.
 */

public class SolidExplorerUrlReplacerSettings extends ListCheckActivity {

    private void showEditDialog(final ListCheckActivity.DataProvider.ListItem item) {
        final View view = getLayoutInflater().inflate(R.layout.dialog_url_replacer, null, false);
        final EditText editFrom = (EditText) view.findViewById(R.id.from);
        final EditText editTo = (EditText) view.findViewById(R.id.to);

        if (item != null) {
            DataProvider.Data data = item.getData(DataProvider.Data.class);
            editFrom.setText(data.mFrom);
            editTo.setText(data.mTo);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.solid_explorer_url_replacer_dialog_title)
                .setView(view)
                .setNegativeButton(R.string.app_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getDataProvider().updateData(item, null);
                    }
                })
                .setPositiveButton(R.string.app_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String to = editTo.getText().toString();
                        getDataProvider().updateData(item, to);
                    }
                })
                .create()
                .show();
    }

    private DataProvider getDataProvider() {
        return (DataProvider) mDataProvider;
    }

    @Override
    protected Class<? extends ListCheckActivity.DataProvider> getDataProviderClass() {
        return DataProvider.class;
    }

    public static class DataProvider extends ListCheckActivity.DataProvider {

        class Data {
            String mFrom;
            String mTo;
            boolean mIsInDb;

            Data(String from, String to, boolean isInDb) {
                mFrom = from;
                mTo = to;
                mIsInDb = isInDb;
            }

            @Override
            public String toString() {
                return "Data, from: " + mFrom + ", to: " + mTo  + ", isInDb: " + mIsInDb;
            }
        }

        private static Map<String, String> sDbPackageNameToProtocol = new HashMap<String, String>() {{
            put("pl.solidexplorer.plugins.network.smb:1:0", "smb");
            put("pl.solidexplorer.plugins.network.dav:1:0", "dav");
            put("pl.solidexplorer.plugins.network.ftp:1:0", "ftp");
            put("pl.solidexplorer.plugins.network.ftp:1:1", "sftp");
        }};
        private List<Data> mDataList;

        public DataProvider(ListCheckActivity activity) {
            super(activity);
            Cursor cursor = activity.getContentResolver().query(Uri.parse("content://pl.solidexplorer2.files/db/explorer/file_systems"),
                    new String[] { "package_name", "server", "port", "path" }, null, null, null);
            if (cursor == null) {
                Toast.makeText(activity, R.string.solid_explorer_db_query_error, Toast.LENGTH_LONG).show();
                throw new RuntimeException("Empty query cursor.");
            }

            Set<String> replacers = Prefs.instance().getStringSet(R.string.key_solid_explorer_url_replacers, new HashSet<String>());
            Map<String, String> storedReplacers = new HashMap<>(replacers.size());
            for (String replacer : replacers) {
                JSONObject jReplacer = (JSONObject) JSON.parse(replacer);
                String from = jReplacer.getString("from");
                String to = jReplacer.getString("to");
                storedReplacers.put(from, to);
            }

            mDataList = new ArrayList<>();
            while (cursor.moveToNext()) {
                if (cursor.getString(0) == null) {
                    Toast.makeText(activity, R.string.solid_explorer_db_query_error, Toast.LENGTH_LONG).show();
                    throw new RuntimeException("Cursor getString return null.");
                }
                String protocol = sDbPackageNameToProtocol.get(cursor.getString(0));
                if (protocol == null) {
                    continue;
                }
                String server = cursor.getString(1);
                int port = cursor.getInt(2);
                String path = cursor.getString(3);
                if (path == null) {
                    path = "";
                }
                path = StringUtils.stripStart(path, "/");

                String from = protocol + "://" + server + (port == 0 ? "" : (":" + port)) + "/" + path;
                String to = storedReplacers.get(from);
                if (to != null) {
                    storedReplacers.remove(from);
                } else {
                    to = "";
                }
                Logger.v("Replacer(Db): " + from + " -> " + to);
                mDataList.add(new Data(from, to, true));
            }
            cursor.close();
            if (mDataList.isEmpty()) {
                Toast.makeText(activity, R.string.solid_explorer_db_empty, Toast.LENGTH_LONG).show();
                throw new RuntimeException("Empty db");
            }

            for (Map.Entry<String, String> kv : storedReplacers.entrySet()) {
                Logger.v("Replacer(Stored): " + kv.getKey() + " -> " + kv.getValue());
                mDataList.add(new Data(kv.getKey(), kv.getValue(), false));
            }
        }

        public void updateData(ListItem item, String to) {
            Logger.i("Url replacer update data, item: " + item + ", to: " + to);
            Data data = item.getData(Data.class);
            if (StringUtils.isBlank(to) && !data.mIsInDb) {
                mDataList.remove(data);
            } else {
                data.mTo = StringUtils.isBlank(to) ? "" : to;
            }

            Set<String> storedItems = new HashSet<>();
            for (Data data_ : mDataList) {
                if (StringUtils.isBlank(data_.mTo)) {
                    continue;
                }
                JSONObject jReplacer = new JSONObject();
                jReplacer.put("from", data_.mFrom);
                jReplacer.put("to", data_.mTo);
                storedItems.add(jReplacer.toJSONString());
            }
            if (storedItems.size() > 0) {
                Prefs.instance().edit()
                        .putStringSet(R.string.key_solid_explorer_url_replacers, storedItems)
                        .commit();
            } else {
                Prefs.instance().edit()
                        .remove(R.string.key_solid_explorer_url_replacers)
                        .commit();
            }

            notifyDataSetChanged();
        }

        private SolidExplorerUrlReplacerSettings getActivity() {
            return (SolidExplorerUrlReplacerSettings) mActivity;
        }

        @Override
        protected String getActivityTitle() {
            return mActivity.getString(R.string.pref_solid_explorer_replace_url);
        }

        @Override
        protected String[] getTabTitles() {
            return new String[] { mActivity.getString(R.string.list) };
        }

        @Override
        protected int getListItemCount(int tab) {
            return mDataList.size();
        }

        @Override
        protected ListItem getListItem(int tab, int position) {
            Data data = mDataList.get(position);
            ListItem item = new ListItem();
            item.mData = data;
            item.mIcon = mActivity.getResources().getDrawable(R.drawable.ic_computer);
            if (!data.mIsInDb) {
                item.mIcon.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
            }
            item.mTitle = data.mFrom;
            item.mDescription = data.mTo;
            return item;
        }

        @Override
        protected boolean reload() {
            return false;
        }

        @Override
        protected boolean hideCheckBox() {
            return true;
        }

        @Override
        public void onItemClick(ListItem item) {
            getActivity().showEditDialog(item);
        }
    }
}
