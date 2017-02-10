package li.lingfeng.ltweaks.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import static li.lingfeng.ltweaks.utils.ContextUtils.dp2px;

/**
 * Created by smallville on 2017/2/6.
 */

public class SimpleDrawer extends DrawerLayout {

    protected LinearLayout mNavLayout;
    protected LinearLayout mHeaderLayout;
    protected ImageView mHeaderImage;
    protected TextView mHeaderText;

    protected ListView mNavList;
    protected NavListAdapter mNavListAdapter;
    protected NavItem[] mNavItems;
    protected Drawable mAppIcon;
    protected String mAppName;

    public SimpleDrawer(Context context, View mainView, NavItem[] navItems,
                        Drawable appIcon, String appName) {
        super(context);
        mNavItems = navItems;
        mAppIcon  = appIcon;
        mAppName  = appName;

        addView(mainView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mNavLayout = new LinearLayout(getContext());
        mNavLayout.setOrientation(LinearLayout.VERTICAL);
        mNavLayout.setBackgroundColor(Color.WHITE);

        createHeaderView();
        createListView();

        LayoutParams params = new LayoutParams(dp2px(320), LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.LEFT;
        addView(mNavLayout, params);
    }

    protected void createHeaderView() {
        mHeaderLayout = new LinearLayout(getContext());
        GradientDrawable bgGradient = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[] { Color.parseColor("#81C784"), Color.parseColor("#4CAF50"), Color.parseColor("#2E7D32") });
        mHeaderLayout.setBackgroundDrawable(bgGradient);
        mHeaderLayout.setGravity(Gravity.BOTTOM);
        mHeaderLayout.setOrientation(LinearLayout.VERTICAL);
        int padding = dp2px(16);
        mHeaderLayout.setPadding(padding, padding, padding, padding);

        mHeaderImage = new ImageView(getContext());
        mHeaderImage.setPadding(0, padding, 0, 0);
        mHeaderImage.setImageDrawable(mAppIcon);
        mHeaderLayout.addView(mHeaderImage, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        mHeaderText = new TextView(getContext());
        mHeaderText.setPadding(0, padding, 0, 0);
        mHeaderText.setText(mAppName);
        mHeaderText.setTextSize(14);
        mHeaderText.setTextColor(Color.WHITE);
        mHeaderLayout.addView(mHeaderText, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        mNavLayout.addView(mHeaderLayout, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp2px(160)));
    }

    protected void createListView() {
        mNavList = new ListView(getContext());
        mNavListAdapter = new NavListAdapter();
        mNavList.setAdapter(mNavListAdapter);
        mNavList.setOnItemClickListener(mNavListAdapter);
        mNavList.setDividerHeight(0);
        mNavList.setHeaderDividersEnabled(false);
        mNavList.setFooterDividersEnabled(false);
        mNavLayout.addView(mNavList, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public static class NavItem {
        public Drawable mIcon;
        public String mText;
        public View mClickView;

        public NavItem(Drawable icon, String text, View clickView) {
            mIcon = icon;
            mText = text;
            mClickView = clickView;
        }
    }

    private class NavListAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {

        private View[] mNavItemViews = new View[getCount()];

        @Override
        public int getCount() {
            return mNavItems.length;
        }

        @Override
        public NavItem getItem(int position) {
            return mNavItems[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = mNavItemViews[position];
            if (view != null) {
                return view;
            }

            TextView textView = new TextView(getContext());
            NavItem navItem = getItem(position);
            textView.setText(navItem.mText);
            navItem.mIcon.setColorFilter(0xFF7B7B7B, PorterDuff.Mode.SRC_ATOP);
            textView.setCompoundDrawablesWithIntrinsicBounds(navItem.mIcon, null, null, null);

            textView.setTextColor(Color.parseColor("#FF212121"));
            textView.setTextSize(14f);
            textView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            textView.setGravity(Gravity.CENTER_VERTICAL);
            textView.setPadding(dp2px(16f), dp2px(12f), 0, dp2px(12f));
            textView.setMinHeight(dp2px(48f));
            textView.setSingleLine();
            textView.setCompoundDrawablePadding(dp2px(32f));

            view = textView;
            mNavItemViews[position] = view;
            return view;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            getItem(position).mClickView.performClick();
            closeDrawers();
        }
    }
}
