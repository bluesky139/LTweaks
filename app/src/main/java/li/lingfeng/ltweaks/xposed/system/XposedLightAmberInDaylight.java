package li.lingfeng.ltweaks.xposed.system;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.animation.AnimationUtils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2018/1/26.
 * https://github.com/aosp-mirror/platform_frameworks_base/blob/nougat-mr2.3-release/services/core/java/com/android/server/display/NightDisplayService.java
 */
@XposedLoad(packages = PackageNames.ANDROID, prefs = R.string.key_display_light_amber_in_daylight)
public class XposedLightAmberInDaylight extends XposedBase {

    private static final String NIGHT_DISPLAY_SERVICE = "com.android.server.display.NightDisplayService";
    private static final int LEVEL_COLOR_MATRIX_NIGHT_DISPLAY = 100;
    private static final float[] MATRIX_LIGHT_AMBER = new float[] { // ~= 5500K
            1,      0,      0, 0,
            0, 0.933f,      0, 0,
            0,      0, 0.870f, 0,
            0,      0,      0, 1
    };
    private boolean mNeedSet = false;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(NIGHT_DISPLAY_SERVICE, "onActivated", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                boolean activated = (boolean) param.args[0];
                if (activated) {
                    return;
                }
                Boolean mIsActivated = (Boolean) XposedHelpers.getObjectField(param.thisObject, "mIsActivated");
                if (mIsActivated == null || mIsActivated != activated) {
                    mNeedSet = true;
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (mNeedSet) {
                    mNeedSet = false;
                    Logger.i("Set light amber in daylight.");
                    setLightAmberInDaylight(param.thisObject);
                }
            }
        });
    }

    private void setLightAmberInDaylight(final Object thisObject) throws Throwable {
        ValueAnimator mColorMatrixAnimator = (ValueAnimator) XposedHelpers.getObjectField(thisObject, "mColorMatrixAnimator");
        if (mColorMatrixAnimator != null) {
            mColorMatrixAnimator.cancel();
        }

        Class cls = findClass("com.android.server.display.DisplayTransformManager");
        final Object dtm = XposedHelpers.callMethod(thisObject, "getLocalService", cls);
        final float[] from = (float[]) XposedHelpers.callMethod(dtm, "getColorMatrix", LEVEL_COLOR_MATRIX_NIGHT_DISPLAY);
        final float[] to = MATRIX_LIGHT_AMBER;

        final TypeEvaluator COLOR_MATRIX_EVALUATOR = (TypeEvaluator) XposedHelpers.getStaticObjectField(thisObject.getClass(), "COLOR_MATRIX_EVALUATOR");
        final Object MATRIX_IDENTITY = XposedHelpers.getStaticObjectField(thisObject.getClass(), "MATRIX_IDENTITY");
        mColorMatrixAnimator = ValueAnimator.ofObject(COLOR_MATRIX_EVALUATOR,
                from == null ? MATRIX_IDENTITY : from, to);
        Context context = (Context) XposedHelpers.callMethod(thisObject, "getContext");
        mColorMatrixAnimator.setDuration(context.getResources()
                .getInteger(android.R.integer.config_longAnimTime));
        mColorMatrixAnimator.setInterpolator(AnimationUtils.loadInterpolator(
                context, android.R.interpolator.fast_out_slow_in));
        mColorMatrixAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                final float[] value = (float[]) animator.getAnimatedValue();
                XposedHelpers.callMethod(dtm, "setColorMatrix", LEVEL_COLOR_MATRIX_NIGHT_DISPLAY, value);
            }
        });
        mColorMatrixAnimator.addListener(new AnimatorListenerAdapter() {

            private boolean mIsCancelled;

            @Override
            public void onAnimationCancel(Animator animator) {
                mIsCancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (!mIsCancelled) {
                    // Ensure final color matrix is set at the end of the animation. If the
                    // animation is cancelled then don't set the final color matrix so the new
                    // animator can pick up from where this one left off.
                    XposedHelpers.callMethod(dtm, "setColorMatrix", LEVEL_COLOR_MATRIX_NIGHT_DISPLAY, to);
                }
                XposedHelpers.setObjectField(thisObject, "mColorMatrixAnimator", null);
            }
        });
        mColorMatrixAnimator.start();
    }
}
