package li.lingfeng.ltweaks.xposed.google;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.prefs.Prefs;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.Utils;
import li.lingfeng.ltweaks.utils.ViewUtils;
import li.lingfeng.ltweaks.utils.XposedUtils;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/9/4.
 * Reference: https://code.highspec.ru/Mikanoshi/YourTubePlus/blob/master/src/name/mikanoshi/yourtubeplus/XposedMod.java
 */
@XposedLoad(packages = PackageNames.YOUTUBE, prefs = {})
public class XposedYoutubeSetQuality extends XposedBase {

    private Set<XC_MethodHook.Unhook> mActivityAttachHook;
    private ClassTester mClassTester;
    private boolean mSetQualityHooked = false;
    private int[] mCurrentQualities;
    private WeakReference<Object> mItemClickRef; // gnb

    private static final String WATCH_WHILE_ACTIVITY = "com.google.android.apps.youtube.app.WatchWhileActivity";
    private WeakReference<TextView> mTitleViewRef;
    private String mLastTitle;

    @Override
    protected void handleLoadPackage() throws Throwable {
        if (Prefs.instance().getIntFromString(R.string.key_youtube_set_quality, 0) <= 0) {
            return;
        }

        mActivityAttachHook = hookAllMethods(Activity.class, "attach", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (mActivityAttachHook != null) {
                    XposedUtils.unhookAll(mActivityAttachHook);
                    mActivityAttachHook = null;
                    startHook((Activity) param.thisObject);
                }
            }
        });
    }

    private void startHook(Activity activity) throws Throwable {
        try {
            mClassTester = new ClassTester();
            mClassTester.createEmptyResult();
            boolean match = Utils.loadObfuscatedClasses(mClassTester.mResultItemClick, activity, "ltweaks_result_item_click", ClassTester._VER, lpparam.classLoader);
            if (!match) {
                Logger.e("No match obfuscated classes.");
                return;
            }
        } catch (Throwable e) {
            Logger.w("Can't load obfuscated classes, " + e);
            long startTime = System.currentTimeMillis();
            mClassTester = new ClassTester();
            mClassTester.startTest();
            Logger.d("Time cost on test obfuscated classes " + (System.currentTimeMillis() - startTime));

            Utils.saveObfuscatedClasses(mClassTester.mResultItemClick, activity, "ltweaks_result_item_click", ClassTester._VER);
            if (mClassTester.mResultItemClick == null) {
                return;
            }
        }

        XposedBridge.hookMethod(mClassTester.mResultItemClick.mMethodUpdateList, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!mSetQualityHooked) {
                    mSetQualityHooked = true;
                    hookSetQualityFromUpdateListMethod(param);
                }

                Object[] qjqArray = (Object[]) param.args[0];
                int index = (int) param.args[1];
                mCurrentQualities = new int[qjqArray.length];
                Pattern pattern = Pattern.compile("\\d{3,}");
                for (int i = 0; i < qjqArray.length; ++i) {
                    Matcher matcher = pattern.matcher(qjqArray[i].toString());
                    if (matcher.find()) {
                        mCurrentQualities[i] = Integer.valueOf(matcher.group(0));
                    } else {
                        mCurrentQualities[i] = 0;
                    }
                }
                mItemClickRef = new WeakReference<>(param.thisObject);
                Logger.d("Qualities are total " + mCurrentQualities.length + ", set by index " + index);

            }
        });

        findAndHookActivity(WATCH_WHILE_ACTIVITY, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                final ViewGroup rootView = (ViewGroup) activity.findViewById(android.R.id.content);
                rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mTitleViewRef != null) {
                            TextView titleView = mTitleViewRef.get();
                            if (titleView != null) {
                                if (!titleView.getText().toString().equals(mLastTitle)) {
                                    mLastTitle = titleView.getText().toString();
                                    Logger.d("New video");
                                    setQuality();
                                }
                                return;
                            }
                        }

                        ViewGroup videoLayout = (ViewGroup) ViewUtils.findViewByName(rootView, "video_info_loading_layout");
                        if (videoLayout != null) {
                            View expandView = ViewUtils.findViewByName(videoLayout, "expand_click_target");
                            if (expandView != null) {
                                TextView titleView = (TextView) ViewUtils.findViewByName((ViewGroup) expandView.getParent(), "title");
                                mTitleViewRef = new WeakReference<>(titleView);
                                mLastTitle = titleView.getText().toString();
                                Logger.d("New video.");
                                setQuality();
                            }
                        }
                    }
                });
            }
        });
    }

    private void hookSetQualityFromUpdateListMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        Object x = mClassTester.mResultItemClick.mFieldToSetquality.get(param.thisObject);
        Logger.d("Cls set quality " + x.getClass());
        Object[] parameterTypes = mClassTester.mResultItemClick.mMethodSetQuality.getParameterTypes();
        Object[] parameterTypesAndCallback = new Object[parameterTypes.length + 1];
        System.arraycopy(parameterTypes, 0, parameterTypesAndCallback, 0, parameterTypes.length);
        parameterTypesAndCallback[parameterTypes.length] = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.d("Set quality " + param.args[0]);
            }
        };
        findAndHookMethod(x.getClass(), mClassTester.mResultItemClick.mMethodSetQuality.getName(), parameterTypesAndCallback);
    }

    private void setQuality() {
        try {
            int maxQuality = Prefs.instance().getIntFromString(R.string.key_youtube_set_quality, 0);
            if (maxQuality > 0) {
                int quality = 0;
                for (int i = mCurrentQualities.length - 1; i > 0; --i) {
                    int _quality = mCurrentQualities[i];
                    if (maxQuality >= _quality) {
                        quality = _quality;
                        break;
                    }
                }
                if (quality > 0) {
                    Logger.i("Change quality at start, " + quality);
                    mClassTester.mResultItemClick.mMethodSetQuality.invoke(
                            mClassTester.mResultItemClick.mFieldToSetquality.get(mItemClickRef.get()),
                            quality
                    );
                } else {
                    Logger.e("No quality match in current qualities?");
                }
            }
        } catch (Throwable e) {
            Logger.e("Can't set quality, " + e);
        }
    }

    // Obfuscated classes below are v12.23.60
    class ClassTester {
        int mErrorCount = 0;
        static final int MAX_ERROR_COUNT = 50;
        static final int CHECKED_ITEM_CLICK_FIELD_INT = 0x1; // int W
        static final int _VER = 1;
        ResultItemClick mResultItemClick;

        class Result {
            void makeSureAllChecked() throws Throwable {
                Field[] fields = getClass().getDeclaredFields();
                for (Field field : fields) {
                    Object o = field.get(this);
                    if (o == null) {
                        throw new Exception(toString());
                    }
                }
            }

            @Override
            public String toString() {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    Field[] fields = getClass().getDeclaredFields();
                    for (Field field : fields) {
                        field.setAccessible(true);
                        if (field.getName().startsWith("this")) {
                            continue;
                        }
                        stringBuilder.append(field.getName());
                        stringBuilder.append(" ");
                        stringBuilder.append(field.get(this));
                        stringBuilder.append(", ");
                    }
                    return stringBuilder.toString();
                } catch (Throwable e) {
                    return e.toString();
                }
            }
        }

        class ResultItemClick extends Result {
            Class mClsItemClick; // gnb
            Class mClsQuality; // qjq
            Field mFieldToSetquality; // gnb.X
            Method mMethodSetQuality; // wmy.a(), interface
            Method mMethodUpdateList; // gnb.a(qjq[], int)
            int mChecked = 0;

            @Override
            void makeSureAllChecked() throws Throwable {
                super.makeSureAllChecked();
                if (mChecked != (CHECKED_ITEM_CLICK_FIELD_INT)) {
                    throw new Exception("mChecked 0x" + Integer.toHexString(mChecked));
                }
            }
        }

        void createEmptyResult() {
            mResultItemClick = new ResultItemClick();
        }

        void startTest() {
            long startTime = System.currentTimeMillis();
            Utils.ObfuscatedClassGenerator g = new Utils.ObfuscatedClassGenerator("", 4);
            while (g.hasNext()) {
                String clsName = g.next();
                try {
                    if (mResultItemClick == null) {
                        mResultItemClick = testClassImplementedItemClick(clsName);
                    }
                    if (mResultItemClick != null) {
                        break;
                    }
                } catch (XposedHelpers.ClassNotFoundError e) {
                    Logger.v("class not found, " + clsName);
                    ++mErrorCount;
                } catch (Throwable e) {
                    Logger.v("test class failed, " + clsName + ", " + e);
                }
                if (mErrorCount > MAX_ERROR_COUNT) {
                    Logger.e("test class error count > " + MAX_ERROR_COUNT + ", abort.");
                    break;
                }
            }
            Logger.i("Test class mResultItemClick " + mResultItemClick);
            Logger.i("Test class cost time " + (System.currentTimeMillis() - startTime));
        }

        ResultItemClick testClassImplementedItemClick(String name) throws Throwable {
            Class cls = findClass(name);
            if (!AdapterView.OnItemClickListener.class.isAssignableFrom(cls)) {
                return null;
            }
            Logger.v("Test cls " + cls + " implemented OnItemClickListener.");
            ResultItemClick result = new ResultItemClick();
            result.mClsItemClick = cls;

            Field[] fields = cls.getDeclaredFields();
            for (Field field : fields) {
                if (field.getType() == int.class) {
                    result.mChecked |= CHECKED_ITEM_CLICK_FIELD_INT;
                } else if (Object[].class.isAssignableFrom(field.getType())) {
                    result.mClsQuality = field.getType().getComponentType();
                    if (!Parcelable.class.isAssignableFrom(result.mClsQuality)
                            || !Comparable.class.isAssignableFrom(result.mClsQuality)) {
                        throw new Exception("mClsQuality is not Parcelable or Comparable.");
                    }
                    Field fieldCreator = XposedHelpers.findField(result.mClsQuality, "CREATOR");
                    if (fieldCreator.getType() != Parcelable.Creator.class
                            || !Modifier.isPublic(fieldCreator.getModifiers())
                            || !Modifier.isStatic(fieldCreator.getModifiers())
                            || !Modifier.isFinal(fieldCreator.getModifiers())) {
                        throw new Exception("mClsQuality creator not match.");
                    }
                    XposedHelpers.findFirstFieldByExactType(result.mClsQuality, int.class);
                    XposedHelpers.findFirstFieldByExactType(result.mClsQuality, String.class);
                } else if (field.getType().isInterface()) {
                    result.mFieldToSetquality = field;
                    result.mFieldToSetquality.setAccessible(true);
                    result.mMethodSetQuality = XposedHelpers.findMethodsByExactParameters(
                            field.getType(), void.class, int.class)[0];
                    result.mMethodSetQuality.setAccessible(true);
                }
            }

            if (result.mClsQuality == null) {
                throw new Exception("mClsQuality is null.");
            }

            Class clsQualityArray = findClass("[L" + result.mClsQuality.getName() + ";");
            result.mMethodUpdateList = XposedHelpers.findMethodsByExactParameters(cls,
                    void.class, clsQualityArray, int.class)[0];
            result.mMethodUpdateList.setAccessible(true);

            result.makeSureAllChecked();
            return result;
        }
    }
}
