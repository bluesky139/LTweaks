package li.lingfeng.ltweaks.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Pair;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.prefs.Prefs;
import li.lingfeng.ltweaks.utils.Logger;

/**
 * Created by lilingfeng on 2017/7/7.
 */

public class SolidExplorerUrlReplacerSettings extends ListCheckActivity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem menuAdd = menu.add("Add");
        menuAdd.setIcon(android.R.drawable.ic_menu_add);
        menuAdd.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        showEditDialog(null);
        return true;
    }

    private void showEditDialog(final ListCheckActivity.DataProvider.ListItem item) {
        final View view = getLayoutInflater().inflate(R.layout.dialog_url_replacer, null, false);
        final EditText editFrom = (EditText) view.findViewById(R.id.from);
        final EditText editTo = (EditText) view.findViewById(R.id.to);

        if (item != null) {
            Pair<String, String> pair = (Pair<String, String>) item.mData;
            String from = pair.first;
            String to = pair.second;
            editFrom.setText(from);
            editTo.setText(to);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.solid_explorer_url_replacer_dialog_title)
                .setView(view)
                .setNegativeButton(R.string.app_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (item != null) {
                            getDataProvider().updateData(item, null, null);
                        }
                    }
                })
                .setPositiveButton(R.string.app_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String from = editFrom.getText().toString();
                        String to = editTo.getText().toString();
                        if (StringUtils.isAnyBlank(from, to)) {
                            Toast.makeText(SolidExplorerUrlReplacerSettings.this, R.string.error, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        getDataProvider().updateData(item, from, to);
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

        private List<ListItem> mListItems;

        public DataProvider(ListCheckActivity activity) {
            super(activity);
            Set<String> replacers = Prefs.instance().getStringSet(R.string.key_solid_explorer_url_replacers, new HashSet<String>());
            mListItems = new ArrayList<>(replacers.size());
            for (String replacer : replacers) {
                JSONObject jReplacer = (JSONObject) JSON.parse(replacer);
                String from = jReplacer.getString("from");
                String to = jReplacer.getString("to");

                ListItem item = createItem(from, to);
                mListItems.add(item);
            }
        }

        private ListItem createItem(String from, String to) {
            ListItem item = new ListItem();
            item.mData = Pair.create(from, to);
            item.mIcon = mActivity.getResources().getDrawable(R.drawable.ic_computer);
            item.mTitle = from;
            item.mDescription = to;
            return item;
        }

        public void updateData(ListItem item, String from, String to) {
            Logger.i("Url replacer update data, item: " + item + ", from: " + from + ", to: " + to);
            if (item == null) { // Add
                item = createItem(from, to);
                mListItems.add(item);
            } else { // Modify
                item.mData = Pair.create(from, to);
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
            return mListItems.size();
        }

        @Override
        protected ListItem getListItem(int tab, int position) {
            return mListItems.get(position);
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
