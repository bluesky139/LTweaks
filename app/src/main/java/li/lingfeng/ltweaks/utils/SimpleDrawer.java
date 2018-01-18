package li.lingfeng.ltweaks.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;

import java.io.File;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.prefs.ActivityRequestCode;

import static li.lingfeng.ltweaks.utils.ContextUtils.dp2px;

/**
 * Created by smallville on 2017/2/6.
 */

public class SimpleDrawer extends DrawerLayout implements DrawerLayout.DrawerListener {

    protected LinearLayout mNavLayout;
    protected LinearLayout mHeaderLayout;
    protected ImageView mHeaderImage;
    protected TextView mHeaderText;

    protected ListView mNavList;
    protected NavListAdapter mNavListAdapter;
    protected NavItem[] mNavItems;
    protected NavItem mHeaderItem;

    protected int mHeaderBackgrondRequestCode = ActivityRequestCode.DRAWER_SELECT_HEADER_BACKGROUND;
    protected Callback.C0 mHeaderBackgroundChangeCallback;
    protected boolean mIsCustomHeaderBackground = false;
    protected int mDefaultBackgroundColor = Color.parseColor("#4CAF50");

    public SimpleDrawer(Activity activity, View mainView, NavItem[] navItems, NavItem headerItem) {
        this(activity, mainView, navItems, headerItem, false);
    }

    public SimpleDrawer(Activity activity, View mainView, NavItem[] navItems, NavItem headerItem,
                        boolean useCircleHeaderImage) {
        this(activity, mainView, navItems, headerItem, useCircleHeaderImage, null);
    }

    public SimpleDrawer(Activity activity, View mainView, NavItem[] navItems, NavItem headerItem,
                        boolean useCircleHeaderImage, Object headerBackgroundClick) {
        super(activity);
        mNavItems = navItems;
        mHeaderItem = headerItem;

        addView(mainView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mNavLayout = new LinearLayout(getContext());
        mNavLayout.setOrientation(LinearLayout.VERTICAL);
        mNavLayout.setBackgroundColor(Color.WHITE);

        createHeaderView(useCircleHeaderImage, headerBackgroundClick);
        createListView();

        LayoutParams params = new LayoutParams(dp2px(280), LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.LEFT;
        addView(mNavLayout, params);
        addDrawerListener(this);

        updateCustomHeaderBackground();
        hookOnActivityResult();
    }

    protected void createHeaderView(boolean useCircleHeaderImage, final Object headerBackgroundClick) {
        mHeaderLayout = new LinearLayout(getContext());
        mHeaderLayout.setBackgroundColor(Color.parseColor("#4CAF50"));
        mHeaderLayout.setGravity(Gravity.BOTTOM);
        mHeaderLayout.setOrientation(LinearLayout.VERTICAL);
        int padding = dp2px(16);
        mHeaderLayout.setPadding(padding, padding, padding, padding);

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
        mHeaderLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.i("Drawer header background is clicked.");
                if (headerBackgroundClick == null) {
                    defaultHeaderBackgroundClick();
                    return;
                }
                if (headerBackgroundClick instanceof View) {
                    ((View) headerBackgroundClick).performClick();
                } else if (headerBackgroundClick instanceof View.OnClickListener) {
                    ((View.OnClickListener) headerBackgroundClick).onClick(v);
                } else {
                    Logger.e("Unknown type of click obj on drawer header background.");
                }
            }
        });

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

    protected void updateCustomHeaderBackground() {
        String headerBackgroundPath = getDrawerHeaderBackgroundPath();
        if (new File(headerBackgroundPath).exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(headerBackgroundPath);
            BitmapDrawable drawable = new BitmapDrawable(getResources(), bitmap);
            updateHeaderBackground(drawable);
            mIsCustomHeaderBackground = true;
        }
    }

    protected String getDrawerHeaderBackgroundPath() {
        return getContext().getFilesDir() + "/ltweaks_drawer_header_background";
    }

    public void updateHeaderBackground(Drawable drawable) {
        if (!mIsCustomHeaderBackground) {
            mHeaderLayout.setBackgroundDrawable(drawable);
        }
    }

    public void updateHeaderBackground(@ColorInt int color) {
        mDefaultBackgroundColor = color;
        if (!mIsCustomHeaderBackground) {
            mHeaderLayout.setBackgroundColor(color);
        }
    }

    public void updateNavListBackground(@ColorInt int color) {
        mNavList.setBackgroundColor(color);
    }

    public void updateNavListTextColor(@ColorInt int color) {
        mNavListAdapter.setTextColor(color);
    }

    public void updateClickObjs(Object[] clickObjs) {
        for (int i = 0; i < clickObjs.length; ++i) {
            mNavItems[i].mClickObj = clickObjs[i];
        }
    }

    public void setHeaderBackgroundRequestCode(int code) {
        mHeaderBackgrondRequestCode = code;
    }

    public void setHeaderBackgroundChangeCallback(Callback.C0 callback) {
        mHeaderBackgroundChangeCallback = callback;
    }

    protected void defaultHeaderBackgroundClick() {
        new AlertDialog.Builder(getContext())
                .setItems(new String[]{"Use default background", "Select a custom background"},
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    mIsCustomHeaderBackground = false;
                                    Bitmap oldBitmap = null;
                                    if (mHeaderLayout.getBackground() instanceof BitmapDrawable) {
                                        oldBitmap = ((BitmapDrawable) mHeaderLayout.getBackground()).getBitmap();
                                    }
                                    //int color = ContextUtils.getColorFromTheme(getContext().getTheme(), "colorPrimary");
                                    updateHeaderBackground(mDefaultBackgroundColor);
                                    if (oldBitmap != null) {
                                        oldBitmap.recycle();
                                    }
                                    File file = new File(getDrawerHeaderBackgroundPath());
                                    if (file.exists()) {
                                        file.delete();
                                    }
                                } else {
                                    ContextUtils.selectPicture(getActivity(), ActivityRequestCode.DRAWER_SELECT_HEADER_BACKGROUND);
                                }
                            }
                        })
                .create()
                .show();
    }

    protected void hookOnActivityResult() {
        XposedHelpers.findAndHookMethod(Activity.class, "dispatchActivityResult", String.class, int.class, int.class, Intent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String who = (String) param.args[0];
                int requestCode = (int) param.args[1];
                if (who == null && requestCode == mHeaderBackgrondRequestCode) {
                    Object fragments = XposedHelpers.getObjectField(param.thisObject, "mFragments");
                    XposedHelpers.callMethod(fragments, "noteStateNotSaved");

                    int resultCode = (int) param.args[2];
                    Intent data = (Intent) param.args[3];
                    if (resultCode == Activity.RESULT_OK) {
                        Uri uri = data.getData();
                        String filepath = getDrawerHeaderBackgroundPath();
                        File file = new File(filepath);
                        Logger.i("Crop and save image " + uri + " to " + filepath);
                        try {
                            Bitmap oldBitmap = null;
                            if (mHeaderLayout.getBackground() instanceof BitmapDrawable) {
                                oldBitmap = ((BitmapDrawable) mHeaderLayout.getBackground()).getBitmap();
                            }
                            Bitmap bitmap = IOUtils.createCenterCropBitmapFromUri(uri,
                                    mHeaderLayout.getWidth(), mHeaderLayout.getHeight());
                            byte[] bytes = IOUtils.bitmap2bytes(bitmap, Bitmap.CompressFormat.JPEG);
                            FileUtils.writeByteArrayToFile(file, bytes);
                            BitmapDrawable drawable = new BitmapDrawable(getResources(), bitmap);
                            updateHeaderBackground(drawable);
                            mIsCustomHeaderBackground = true;
                            if (oldBitmap != null) {
                                oldBitmap.recycle();
                            }
                            if (mHeaderBackgroundChangeCallback != null) {
                                mHeaderBackgroundChangeCallback.onResult();
                            }
                        } catch (Throwable e) {
                            Logger.e("Error to crop and save image, " + e);
                            Logger.stackTrace(e);
                            Toast.makeText(getActivity(), "Error.", Toast.LENGTH_SHORT).show();
                        }
                    }
                    param.setResult(null);
                }
            }
        });
    }

    protected Activity getActivity() {
        return (Activity) getContext();
    }

    public ImageView getHeaderImage() {
        return mHeaderImage;
    }

    public TextView getHeaderText() {
        return mHeaderText;
    }

    public LinearLayout getHeaderLayout() {
        return mHeaderLayout;
    }

    @Override
    public void onDrawerSlide(View drawerView, float slideOffset) {

    }

    @Override
    public void onDrawerOpened(View drawerView) {
        mNavList.requestFocus();
    }

    @Override
    public void onDrawerClosed(View drawerView) {

    }

    @Override
    public void onDrawerStateChanged(int newState) {

    }

    public static class NavItem {
        public Drawable mIcon;
        public CharSequence mText;
        public Object mClickObj;

        public NavItem(Drawable icon, CharSequence text, Object clickObj) throws Throwable {
            if (icon == null || text == null) {
                throw new Exception("NavItem icon " + icon + ", text " + text);
            }
            if (clickObj == null) {
                Logger.w("NavITem " + text + " clickObj is null.");
            }
            mIcon = icon;
            mText = text;
            mClickObj = clickObj;
        }

        public void onClick(View view) {
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

            try {
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
            } catch (Throwable e) {
                Logger.e("SimpleDrawer can't create list item view, " + e);
                Logger.stackTrace(e);

                TextView textView = new TextView(getContext());
                textView.setText("Error!!!");
                view = textView;
            }

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
