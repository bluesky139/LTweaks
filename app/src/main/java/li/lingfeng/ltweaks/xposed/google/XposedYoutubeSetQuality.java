package li.lingfeng.ltweaks.xposed.google;

import android.app.Activity;
import android.content.ComponentCallbacks;
import android.os.Parcelable;
import android.view.View;
import android.widget.AdapterView;

import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.prefs.Prefs;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.Utils;
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
    private boolean mNewVideo = false;

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
            long startTime = System.currentTimeMillis();
            mClassTester = new ClassTester();
            mClassTester.createEmptyResult();
            Utils.loadObfuscatedClasses(mClassTester.mResultItemClick, activity, "ltweaks_result_item_click", ClassTester._VER, lpparam.classLoader);
            Utils.loadObfuscatedClasses(mClassTester.mResultFragment, activity, "ltweaks_result_fragment", ClassTester._VER, lpparam.classLoader);
            Logger.d("time cost aaa " + (System.currentTimeMillis() - startTime));
        } catch (Throwable e) {
            Logger.w("Can't load obfuscated classes, " + e);
            mClassTester = new ClassTester();
            mClassTester.startTest();
            if (mClassTester.mResultItemClick == null || mClassTester.mResultFragment == null) {
                return;
            }
            Utils.saveObfuscatedClasses(mClassTester.mResultItemClick, activity, "ltweaks_result_item_click", ClassTester._VER);
            Utils.saveObfuscatedClasses(mClassTester.mResultFragment, activity, "ltweaks_result_fragment", ClassTester._VER);
        }

        XposedBridge.hookMethod(mClassTester.mResultFragment.mMethodNewVideo, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("New video.");
                mNewVideo = true;
            }
        });

        XposedBridge.hookMethod(mClassTester.mResultItemClick.mMethodUpdateList, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!mSetQualityHooked) {
                    mSetQualityHooked = true;
                    hookSetQualityFromUpdateListMethod(param);
                }

                Object[] qjqArray = (Object[]) param.args[0];
                int index = (int) param.args[1];
                Logger.d("Qualities are total " + qjqArray.length + ", set by index " + index);
                if (mNewVideo) {
                    mNewVideo = false;
                    int quality = Prefs.instance().getIntFromString(R.string.key_youtube_set_quality, 0);
                    if (quality > 0) {
                        int[] qualities = ContextUtils.getIntArrayFromStringArray("youtube_quality_int", ContextUtils.createLTweaksContext());
                        index = Math.min(qjqArray.length - 1, ArrayUtils.indexOf(qualities, quality));
                        quality = qualities[index];
                        Logger.i("Change quality at start, " + quality);
                        mClassTester.mResultItemClick.mMethodSetQuality.invoke(
                                mClassTester.mResultItemClick.mFieldToSetquality.get(param.thisObject),
                                quality
                        );
                    }
                }
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

    // Obfuscated classes below are v12.23.60
    class ClassTester {
        int mErrorCount = 0;
        static final int MAX_ERROR_COUNT = 50;
        static final int CHECKED_ITEM_CLICK_FIELD_INT = 0x1; // int W
        static final int _VER = 1;
        ResultItemClick mResultItemClick;
        ResultFragment mResultFragment;

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

        class ResultFragment extends Result {
            Class mClsFragment; // eho
            Method mMethodNewVideo; // eho.V()
        }

        void createEmptyResult() {
            mResultItemClick = new ResultItemClick();
            mResultFragment = new ResultFragment();
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
                    if (mResultFragment == null) {
                        mResultFragment = testClassFragment(clsName);
                    }
                    if (mResultItemClick != null && mResultFragment != null) {
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
            Logger.i("Test class mResultFragment " + mResultFragment);
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

        ResultFragment testClassFragment(String name) throws Throwable {
            Class cls = findClass(name);
            if (!ComponentCallbacks.class.isAssignableFrom(cls)
                    || !View.OnCreateContextMenuListener.class.isAssignableFrom(cls)) {
                return null;
            }
            Logger.v("Test cls " + cls + " implemented ComponentCallbacks.");
            ResultFragment result = new ResultFragment();
            result.mClsFragment = cls;

            Method[] methods = XposedHelpers.findMethodsByExactParameters(cls, boolean.class);
            for (Method method : methods) {
                if (Modifier.isPrivate(method.getModifiers()) && Modifier.isStatic(method.getModifiers())) {
                    result.mMethodNewVideo = method;
                    result.mMethodNewVideo.setAccessible(true);
                    break;
                }
            }

            result.makeSureAllChecked();
            return result;
        }
    }
}
