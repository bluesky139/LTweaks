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
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Constructor;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.utils.Logger;

/**
 * Created by lilingfeng on 2017/6/23.
 */

public class ListCheckActivity extends AppCompatActivity {

    public static abstract class DataProvider {

        public class ListItem {
            public Drawable mIcon;
            public CharSequence mTitle;
            public CharSequence mDescription;
        }

        protected Activity mActivity;

        public DataProvider(Activity activity) {
            mActivity = activity;
        }

        protected abstract String[] getTabTitles();
        protected abstract int getListItemCount(int tab);
        protected abstract ListItem getListItem(int tab, int position);
    }

    private DataProvider mDataProvider;
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
            Class clsDataProvider = (Class) getIntent().getSerializableExtra("data_provider");
            Constructor constructor = clsDataProvider.getConstructor(Activity.class);
            mDataProvider = (DataProvider) constructor.newInstance(this);
        } catch (Exception e) {
            Toast.makeText(this, "No data provider", Toast.LENGTH_SHORT).show();
            Logger.e("No data provider, " + e);
            finish();
            return;
        }

        setContentView(R.layout.activity_list_check);
        mPagerAdapter = new ListFragmentPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mViewPager.setAdapter(mPagerAdapter);
        mTabLayout = (PagerTabStrip) findViewById(R.id.tabs);
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
            return new ListFragment();
        }

        @Override
        public int getCount() {
            return mDataProvider.getTabTitles().length;
        }
    }

    public static class ListFragment extends Fragment {

        private RecyclerView mListView;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_list_check, container, false);
            mListView = (RecyclerView) view.findViewById(R.id.list);
            mListView.setLayoutManager(new LinearLayoutManager(getActivity()));
            mListView.setAdapter(new ListAdapter());
            return view;
        }

        private class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

            private ListCheckActivity getActivity() {
                return (ListCheckActivity) ListFragment.this.getActivity();
            }

            private DataProvider getDataProvider() {
                return getActivity().mDataProvider;
            }

            private int getTabPosition() {
                return getActivity().mViewPager.getCurrentItem();
            }

            @Override
            public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                ViewHolder viewHolder = new ViewHolder(LayoutInflater.from(getActivity())
                        .inflate(R.layout.list_check, parent, false));
                return viewHolder;
            }

            @Override
            public void onBindViewHolder(final ViewHolder holder, int position) {
                DataProvider.ListItem data = getDataProvider().getListItem(getTabPosition(), position);
                holder.mIcon.setImageDrawable(data.mIcon);
                holder.mTitle.setText(data.mTitle);
                holder.mDescription.setText(data.mDescription);
                holder.itemView.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        holder.mEnabler.toggle();
                    }
                });
            }

            @Override
            public int getItemCount() {
                return getDataProvider().getListItemCount(getTabPosition());
            }

            class ViewHolder extends RecyclerView.ViewHolder {

                public ImageView mIcon;
                public TextView mTitle;
                public TextView mDescription;
                public CheckBox mEnabler;

                public ViewHolder(View view) {
                    super(view);
                    mIcon = (ImageView) view.findViewById(R.id.icon);
                    mTitle = (TextView) view.findViewById(R.id.title);
                    mDescription = (TextView) view.findViewById(R.id.description);
                    mEnabler = (CheckBox) view.findViewById(R.id.enabler);
                }
            }
        }
    }
}
