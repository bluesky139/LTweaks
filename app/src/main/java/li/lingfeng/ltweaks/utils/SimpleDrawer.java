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
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
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
    protected NavItem mHeaderItem;

    public SimpleDrawer(Context context, View mainView, NavItem[] navItems, NavItem headerItem) {
        this(context, mainView, navItems, headerItem, false);
    }

    public SimpleDrawer(Context context, View mainView, NavItem[] navItems, NavItem headerItem,
                        boolean useCircleHeaderImage) {
        super(context);
        mNavItems = navItems;
        mHeaderItem = headerItem;

        addView(mainView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mNavLayout = new LinearLayout(getContext());
        mNavLayout.setOrientation(LinearLayout.VERTICAL);
        mNavLayout.setBackgroundColor(Color.WHITE);

        createHeaderView(useCircleHeaderImage);
        createListView();

        LayoutParams params = new LayoutParams(dp2px(280), LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.LEFT;
        addView(mNavLayout, params);
    }

    protected void createHeaderView(boolean useCircleHeaderImage) {
        mHeaderLayout = new LinearLayout(getContext());
        mHeaderLayout.setBackgroundColor(Color.parseColor("#4CAF50"));
        mHeaderLayout.setGravity(Gravity.BOTTOM);
        mHeaderLayout.setOrientation(LinearLayout.VERTICAL);
        int padding = dp2px(16);
        mHeaderLayout.setPadding(padding, padding, padding, padding);
        mHeaderLayout.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        mHeaderImage = useCircleHeaderImage ? new CircleImageView(getContext()) : new ImageView(getContext());
        mHeaderImage.setPadding(0, padding, 0, 0);
        mHeaderImage.setImageDrawable(mHeaderItem.mIcon);
        mHeaderImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mHeaderItem.onClick(v);
            }
        });
        mHeaderLayout.addView(mHeaderImage, new LinearLayout.LayoutParams(dp2px(64), dp2px(64)));

        mHeaderText = new TextView(getContext());
        mHeaderText.setPadding(0, padding, 0, 0);
        mHeaderText.setText(mHeaderItem.mText);
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

    public void updateDrawerColor(@ColorInt int color) {
        mHeaderLayout.setBackgroundColor(color);
    }

    public void updateDrawerColor(@ColorInt int color, @ColorInt int listColor, @ColorInt int textColor) {
        mHeaderLayout.setBackgroundColor(color);
        mNavList.setBackgroundColor(listColor);
        mNavListAdapter.setTextColor(textColor);
    }

    public void updateClickObjs(Object[] clickObjs) {
        for (int i = 0; i < clickObjs.length; ++i) {
            mNavItems[i].mClickObj = clickObjs[i];
        }
    }

    public ImageView getHeaderImage() {
        return mHeaderImage;
    }

    public static class NavItem {
        Drawable mIcon;
        CharSequence mText;
        Object mClickObj;

        public NavItem(Drawable icon, CharSequence text, Object clickObj) {
            mIcon = icon;
            mText = text;
            mClickObj = clickObj;
        }

        void onClick(View view) {
            Logger.i("Drawer item " + mText + " is clicked.");
            if (mClickObj == null) {
                return;
            }
            if (mClickObj instanceof View) {
                ((View) mClickObj).performClick();
            } else if (mClickObj instanceof View.OnClickListener) {
                ((View.OnClickListener) mClickObj).onClick(view);
            } else {
                Logger.e("Unknown type of click obj in drawer.");
            }
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

        public void setTextColor(@ColorInt int color) {
            for (int i = 0; i < getCount(); ++i) {
                TextView view = (TextView) getView(i, null, null);
                view.setTextColor(color);
            }
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            getItem(position).onClick(view);
            closeDrawers();
        }
    }
}
