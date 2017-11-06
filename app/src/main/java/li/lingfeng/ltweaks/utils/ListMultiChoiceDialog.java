package li.lingfeng.ltweaks.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ListView;

import li.lingfeng.ltweaks.R;

/**
 * Created by smallville on 2017/11/5.
 */

public class ListMultiChoiceDialog implements DialogInterface.OnClickListener {

    public static class Item {
        public String text;
        public String summary;
    }

    public interface OnMultiChoicesChangeListener {
        void onChange(boolean[] choices);
    }

    private Context mContext;
    private Item[] mItems;
    private boolean[] mChoices;
    private OnMultiChoicesChangeListener mListener;
    private ListView mListView;
    private ListAdapter mListAdapter;
    private AlertDialog mDialog;

    public ListMultiChoiceDialog(Context context, CharSequence title, Item[] items,
                                 boolean[] choices, OnMultiChoicesChangeListener listener) {
        mContext = context;
        mItems = items;
        mChoices = choices;
        mListener = listener;
        mListView = new ListView(context);
        mListAdapter = new ListAdapter();
        mListView.setAdapter(mListAdapter);
        mDialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(mListView)
                .setPositiveButton(R.string.app_ok, this)
                .setNegativeButton(R.string.app_cancel, null)
                .create();
    }

    public void show() {
        mDialog.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        mListener.onChange(mChoices);
    }

    class ListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mItems.length;
        }

        @Override
        public Item getItem(int position) {
            return mItems[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.list_multi_choice, null, false);
            final Item item = getItem(position);
            CheckBox checkBox = (CheckBox) view.findViewById(R.id.title_checkbox);
            checkBox.setText(item.text);
            checkBox.setChecked(mChoices[position]);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mChoices[position] = isChecked;
                }
            });
            ImageButton summaryButton = (ImageButton) view.findViewById(R.id.summary_button);
            summaryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewUtils.showDialog(mContext, item.summary);
                }
            });
            return view;
        }
    }
}
