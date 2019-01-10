package li.lingfeng.ltweaks.xposed.communication;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import java.io.File;
import java.lang.ref.WeakReference;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.IOUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.ViewUtils;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/7/31.
 */
@XposedLoad(packages = {
        PackageNames.QQ_LITE,
        PackageNames.QQ,
        PackageNames.QQ_INTERNATIONAL,
        PackageNames.TIM
}, prefs = R.string.key_qq_clear_background)
public class XposedQQChatBackground extends XposedBase {

    private static final String SPLASH_ACTIVITY = "com.tencent.mobileqq.activity.SplashActivity";
    private static final String CHAT_LISTVIEW = "com.tencent.mobileqq.bubble.ChatXListView";
    private BitmapDrawable mLargestDrawable;
    private LruCache<Integer, BitmapDrawable> mBackgroundDrawables; // height -> drawable, consider width is fixed.
    private long mLastModified = 0;
    private WeakReference<ViewGroup> mBackgroundViewRef;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookActivity(lpparam.packageName.equals(PackageNames.TIM) ? SPLASH_ACTIVITY : ClassNames.QQ_CHAT_ACTIVITY,
                "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                File file = new File(getImagePath());
                if (!file.exists()) {
                    Logger.i("Background image file doesn't exist, " + file.getAbsolutePath());
                    return;
                }

                if (mBackgroundDrawables == null) {
                    mBackgroundDrawables = new LruCache<Integer, BitmapDrawable>(5) {
                        @Override
                        protected void entryRemoved(boolean evicted, Integer key, BitmapDrawable oldValue, BitmapDrawable newValue) {
                            Logger.d("entryRemoved " + key);
                            removeBackgroundDrawable(oldValue);
                        }
                    };
                }
                if (mLastModified != file.lastModified()) {
                    mLastModified = file.lastModified();
                    if (mLargestDrawable != null) {
                        removeBackgroundDrawable(mLargestDrawable);
                        mLargestDrawable = null;
                    }
                    mBackgroundDrawables.evictAll();
                }

                final Activity activity = (Activity) param.thisObject;
                final ViewGroup rootView = (ViewGroup) activity.findViewById(android.R.id.content);
                rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        try {
                            handleLayoutChanged(activity);
                        } catch (Throwable e) {
                            Logger.e("Error to handleLayoutChanged, " + e);
                            Logger.stackTrace(e);
                        }
                    }
                });
            }
        });
    }

    private void handleLayoutChanged(Activity activity) throws Throwable {
        ViewGroup viewGroup;
        if (lpparam.packageName.equals(PackageNames.TIM)) {
            final ViewGroup rootView = activity.findViewById(android.R.id.content);
            View chatListView = ViewUtils.findViewByType(rootView, (Class<? extends View>) findClass(CHAT_LISTVIEW));
            if (chatListView == null) {
                return;
            }
            viewGroup = (ViewGroup) chatListView;
        } else {
            int idInputBar = ContextUtils.getIdId("inputBar");
            LinearLayout inputBar = (LinearLayout) activity.findViewById(idInputBar);
            if (inputBar == null) {
                return;
            }
            viewGroup = (ViewGroup) inputBar.getParent();
        }

        int width = viewGroup.getMeasuredWidth();
        int height = viewGroup.getMeasuredHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        BitmapDrawable drawable = null;
        if (mLargestDrawable != null && mLargestDrawable.getBitmap().getHeight() == height) {
            drawable = mLargestDrawable;
        }
        if (drawable == null) {
            drawable = mBackgroundDrawables.get(height);
        }

        if (drawable == null) {
            if (mLargestDrawable != null && mLargestDrawable.getBitmap().getHeight() > height) {
                Bitmap bitmap = IOUtils.bitmapCopy(mLargestDrawable.getBitmap(), 0, 0, width, height);
                if (bitmap == null) {
                    return;
                }
                drawable = new BitmapDrawable(bitmap);
                mBackgroundDrawables.put(height, drawable);
            }

            if (drawable == null) {
                mBackgroundDrawables.evictAll();
                if (mLargestDrawable != null) {
                    removeBackgroundDrawable(mLargestDrawable);
                    mLargestDrawable = null;
                }
                String filepath = getImagePath();
                if (!new File(filepath).exists()) {
                    Logger.e("Can't access file " + filepath);
                    return;
                }
                Bitmap dest = IOUtils.createCenterCropBitmapFromFile(filepath, width, height);
                if (dest == null) {
                    return;
                }
                drawable = new BitmapDrawable(dest);
                mLargestDrawable = drawable;
            }
        }

        if (viewGroup.getBackground() != drawable) {
            Logger.i("Set chat activity background, " + width + "x" + height);
            viewGroup.setBackgroundDrawable(drawable);
            mBackgroundViewRef = new WeakReference<>(viewGroup);
        }
    }

    private void removeBackgroundDrawable(BitmapDrawable drawable) {
        if (mBackgroundViewRef != null) {
            ViewGroup backgroundView = mBackgroundViewRef.get();
            if (backgroundView != null && backgroundView.getBackground() == drawable) {
                backgroundView.setBackgroundColor(Color.TRANSPARENT);
            }
        }
        drawable.getBitmap().recycle();
    }

    private String getImagePath() {
        return Environment.getExternalStoragePublicDirectory("Tencent").getAbsolutePath()
                + "/ltweaks_qq_background";
    }
}
