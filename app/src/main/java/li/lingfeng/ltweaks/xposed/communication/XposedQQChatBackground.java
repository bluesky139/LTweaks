package li.lingfeng.ltweaks.xposed.communication;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.SparseArray;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import java.io.File;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.Utils;
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

    private SparseArray<BitmapDrawable> mBackgroundDrawables; // height -> drawable, consider width is fixed.
    private long mLastModified = 0;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookActivity(ClassNames.QQ_CHAT_ACTIVITY, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                File file = new File(getImagePath());
                if (!file.exists()) {
                    Logger.i("Background image file doesn't exist, " + file.getAbsolutePath());
                    return;
                }

                if (mBackgroundDrawables == null) {
                    mBackgroundDrawables = new SparseArray<>(2);
                }
                if (mLastModified != file.lastModified()) {
                    mLastModified = file.lastModified();
                    mBackgroundDrawables.clear();
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

    private void handleLayoutChanged(Activity activity) {
        int idInputBar = ContextUtils.getIdId("inputBar");
        LinearLayout inputBar = (LinearLayout) activity.findViewById(idInputBar);
        ViewGroup viewGroup = (ViewGroup) inputBar.getParent();
        int width = viewGroup.getMeasuredWidth();
        int height = viewGroup.getMeasuredHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        BitmapDrawable drawable = mBackgroundDrawables.get(height);
        if (drawable == null) {
            for (int i = 0; i < mBackgroundDrawables.size(); ++i) {
                int tmpHeight = mBackgroundDrawables.keyAt(i);
                if (tmpHeight < height) {
                    continue;
                }
                BitmapDrawable tmpDrawable = mBackgroundDrawables.get(tmpHeight);
                Bitmap bitmap = Utils.bitmapCopy(tmpDrawable.getBitmap(), 0, 0, width, height);
                drawable = new BitmapDrawable(bitmap);
                break;
            }

            if (drawable == null) {
                mBackgroundDrawables.clear();
                String filepath = getImagePath();
                if (!new File(filepath).exists()) {
                    Logger.e("Can't access file " + filepath);
                    return;
                }
                Bitmap dest = Utils.createCenterCropBitmapFromFile(filepath, width, height);
                if (dest == null) {
                    return;
                }
                drawable = new BitmapDrawable(dest);
            }
            mBackgroundDrawables.put(height, drawable);
        }

        if (viewGroup.getBackground() != drawable) {
            Logger.i("Set chat activity background, " + width + "x" + height);
            viewGroup.setBackgroundDrawable(drawable);
        }
    }

    private String getImagePath() {
        return Environment.getExternalStoragePublicDirectory("Tencent").getAbsolutePath()
                + "/ltweaks_qq_background";
    }
}
