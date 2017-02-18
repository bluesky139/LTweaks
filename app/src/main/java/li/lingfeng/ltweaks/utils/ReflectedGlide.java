package li.lingfeng.ltweaks.utils;

import android.app.Activity;
import android.widget.ImageView;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;

/**
 * Created by smallville on 2017/2/17.
 */

public class ReflectedGlide {

    private static final String GLIDE = "com.bumptech.glide.Glide";
    private static final String DISK_CACHE_STRATEGY = "com.bumptech.glide.load.engine.DiskCacheStrategy";

    private static Class<?> mClsGlide;
    private static Class<Enum> mClsDiskCacheStrategy;

    private static Method mMethodWith;
    private static Method mMethodLoad;
    private static Method mMethodDiskCacheStrategy;
    private static Method mMethodPlaceHolder;
    private static Method mMethodInto;

    private Object mRequestManager;
    private Object mDrawableTypeRequest;

    public static ReflectedGlide with(Activity activity, ClassLoader classLoader) throws Throwable {
        if (mClsGlide == null) {
            mClsGlide = XposedHelpers.findClass(GLIDE, classLoader);
            mClsDiskCacheStrategy = (Class<Enum>) XposedHelpers.findClass(DISK_CACHE_STRATEGY, classLoader);
        }

        if (mMethodWith == null) {
            mMethodWith = mClsGlide.getDeclaredMethod("with", Activity.class);
        }
        Object reqeustManager = mMethodWith.invoke(null, activity);
        return new ReflectedGlide(reqeustManager);
    }

    private ReflectedGlide(Object requestManager) {
        mRequestManager = requestManager;
    }

    public ReflectedGlide load(String url) throws Throwable {
        if (mMethodLoad == null) {
            mMethodLoad = mRequestManager.getClass().getMethod("load", String.class);
        }
        mDrawableTypeRequest = mMethodLoad.invoke(mRequestManager, url);
        return this;
    }

    public ReflectedGlide diskCacheStrategy(String strategy) throws Throwable {
        if (mMethodDiskCacheStrategy == null) {
            mMethodDiskCacheStrategy = mDrawableTypeRequest.getClass().getMethod("diskCacheStrategy", mClsDiskCacheStrategy);
        }
        Enum e = Enum.valueOf(mClsDiskCacheStrategy, strategy);
        mMethodDiskCacheStrategy.invoke(mDrawableTypeRequest, e);
        return this;
    }

    public ReflectedGlide placeholder(int resId) throws Throwable {
        if (mMethodPlaceHolder == null) {
            mMethodPlaceHolder = mDrawableTypeRequest.getClass().getMethod("placeholder", int.class);
        }
        mMethodPlaceHolder.invoke(mDrawableTypeRequest, resId);
        return this;
    }

    public void into(ImageView imageView) throws Throwable {
        if (mMethodInto == null) {
            mMethodInto = mDrawableTypeRequest.getClass().getMethod("into", ImageView.class);
        }
        mMethodInto.invoke(mDrawableTypeRequest, imageView);
    }
}
