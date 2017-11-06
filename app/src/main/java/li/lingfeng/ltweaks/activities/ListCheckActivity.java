package li.lingfeng.ltweaks.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.buildware.widget.indeterm.IndeterminateCheckBox;
import com.buildware.widget.indeterm.IndeterminateCheckable;

import java.lang.reflect.Constructor;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.ViewUtils;

/**
 * Created by lilingfeng on 2017/6/23.
 */

public class ListCheckActivity extends AppCompatActivity {

    public interface OnItemClickListener {
        void onItemClick(DataProvider.ListItem item);
    }

    public interface OnCheckedChangeListener {
        void onCheckedChanged(DataProvider.ListItem item, Boolean isChecked);
    }

    public static abstract class DataProvider implements OnItemClickListener, OnCheckedChangeListener {

        public class ListItem {
            public Object mData;
            public Drawable mIcon;
            public CharSequence mTitle;
            public CharSequence mDescription;
            public Boolean mChecked; // null is indeterminate

            public <T> T getData(Class<T> cls) {
                return cls.cast(mData);
            }

            @Override
            public String toString() {
                return mData.toString();
            }
        }

        protected ListCheckActivity mActivity;

        public DataProvider(ListCheckActivity activity) {
            mActivity = activity;
        }

        protected abstract String getActivityTitle();
        protected abstract String[] getTabTitles();
        protected abstract int getListItemCount(int tab);
        protected abstract ListItem getListItem(int tab, int position);
        protected abstract boolean reload(); // reload to refresh data list for listview.

        protected boolean hideCheckBox() {
            return false;
        }

        protected boolean linkItemClickToCheckBox() {
            return true;
        }

        @Override
        public void onItemClick(ListItem item) {
        }

        @Override
        public void onCheckedChanged(ListItem item, Boolean isChecked) {
        }

        protected void notifyDataSetChanged() {
            mActivity.notifyDataSetChanged();
        }
    }

    protected DataProvider mDataProvider;
    private ListFragmentPagerAdapter mPagerAdapter;
    private ViewPager mViewPager;
    private PagerTabStrip mTabLayout;

    public static void create(Activity activity, Class<? extends DataProvider> clsDataProvider) {
        Intent intent = new Intent(activity, ListCheckActivity.class);
        intent.putExtra("data_provider", clsDataProvider);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Constructor constructor = getDataProviderClass().getConstructor(ListCheckActivity.class);
            mDataProvider = (DataProvider) constructor.newInstance(this);
        } catch (Exception e) {
            Logger.e("No data provider, " + e);
            Logger.stackTrace(e);
            finish();
            return;
        }

        setTitle(mDataProvider.getActivityTitle());
        setContentView(R.layout.activity_list_check);
        mTabLayout = (PagerTabStrip) findViewById(R.id.tabs);
        mPagerAdapter = new ListFragmentPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.addOnPageChangeListener(new ListFragmentPageChangeListener());
    }

    protected Class<? extends DataProvider> getDataProviderClass() {
        return (Class<? extends DataProvider>) getIntent().getSerializableExtra("data_provider");
    }

    private void notifyDataSetChanged() {
        for (int i = 0; i < mPagerAdapter.getCount(); ++i) {
            ListFragment fragment = (ListFragment) ViewUtils.findFragmentByPosition(
                    getSupportFragmentManager(), mViewPager, i);
            if (fragment != null) {
                fragment.notifyListChanged();
            }
        }
    }

    private class ListFragmentPagerAdapter extends FragmentPagerAdapter {

        public ListFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mDataProvider.getTabTitles()[position];
        }

        @Override
        public Fragment getItem(int position) {
            return ListFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return mDataProvider.getTabTitles().length;
        }
    }

    private class ListFragmentPageChangeListener implements ViewPager.OnPageChangeListener {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            if (mDataProvider.reload()) {
                notifyDataSetChanged();
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }

    public static class ListFragment extends Fragment {

        private RecyclerView mListView;
        private ListAdapter mListAdapter;

        public static ListFragment newInstance(int tab) {
            ListFragment fragment = new ListFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("tab", tab);
            fragment.setArguments(bundle);
            return fragment;
        }

        public int getTab() {
            return getArguments().getInt("tab");
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_list_check, container, false);
            mListView = (RecyclerView) view.findViewById(R.id.list);
            mListView.setLayoutManager(new LinearLayoutManager(getActivity()));
            mListAdapter = new ListAdapter();
            mListView.setAdapter(mListAdapter);
            return view;
        }

        private class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

            private ListCheckActivity getActivity() {
                return (ListCheckActivity) ListFragment.this.getActivity();
            }

            private DataProvider getDataProvider() {
                return getActivity().mDataProvider;
            }

            @Override
            public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                ViewHolder viewHolder = new ViewHolder(LayoutInflater.from(getActivity())
                        .inflate(R.layout.list_check, parent, false));
                return viewHolder;
            }

            @Override
            public void onBindViewHolder(final ViewHolder holder, int position) {
                final DataProvider.ListItem data = getDataProvider().getListItem(getTab(), position);
                holder.mIcon.setImageDrawable(data.mIcon);
                holder.mTitle.setText(data.mTitle);
                holder.mDescription.setText(data.mDescription);

                if (getDataProvider().hideCheckBox()) {
                    holder.mEnabler.setVisibility(View.GONE);
                } else {
                    holder.mEnabler.setOnStateChangedListener(null);
                    holder.mEnabler.setState(data.mChecked);
                    holder.mEnabler.setOnStateChangedListener(new IndeterminateCheckBox.OnStateChangedListener() {
                        @Override
                        public void onStateChanged(IndeterminateCheckBox checkBox, @Nullable Boolean state) {
                            getDataProvider().onCheckedChanged(data, state);
                        }
                    });
                }

                holder.itemView.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (!getDataProvider().hideCheckBox() && getDataProvider().linkItemClickToCheckBox()) {
                            holder.mEnabler.toggle();
                        } else {
                            getDataProvider().onItemClick(data);
                        }
                    }
                });

            }

            @Override
            public int getItemCount() {
                return getDataProvider().getListItemCount(getTab());
            }

            class ViewHolder extends RecyclerView.ViewHolder {

                public ImageView mIcon;
                public TextView mTitle;
                public TextView mDescription;
                public IndeterminateCheckBox mEnabler;

                public ViewHolder(View view) {
                    super(view);
                    mIcon = (ImageView) view.findViewById(R.id.icon);
                    mTitle = (TextView) view.findViewById(R.id.title);
                    mDescription = (TextView) view.findViewById(R.id.description);
                    mEnabler = (IndeterminateCheckBox) view.findViewById(R.id.enabler);
                }
            }
        }

        public void notifyListChanged() {
            mListAdapter.notifyDataSetChanged();
        }
    }
}
