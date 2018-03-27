package li.lingfeng.ltweaks.utils;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.ColorInt;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import static li.lingfeng.ltweaks.utils.ContextUtils.dp2px;

/**
 * Created by smallville on 2017/6/7.
 */

public class SimpleFloatingButton extends ImageButton {

    private static final Interpolator FAST_OUT_SLOW_IN_INTERPOLATOR = new FastOutSlowInInterpolator();
    private static final Interpolator LINEAR_OUT_SLOW_IN_INTERPOLATOR = new LinearOutSlowInInterpolator();
    private static final Interpolator FAST_OUT_LINEAR_IN_INTERPOLATOR = new FastOutLinearInInterpolator();

    public static final int ANIMATED_SCALE         = 0;
    public static final int ANIMATED_TRANSLATION_Y = 1;

    private Activity mActivity;
    private boolean mIsCreated = false;
    private boolean mIsShowed = false;
    private int mAnimatedType = ANIMATED_SCALE;
    private ActivityTouchEventListener mActivityTouchEventListener;

    public static SimpleFloatingButton make(Activity activity) {
        return new SimpleFloatingButton(activity);
    }

    private SimpleFloatingButton(Activity activity) {
        super(activity);
        mActivity = activity;
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(Color.parseColor("#FFDB4437"));
        setBackgroundDrawable(drawable);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int preferredSize = dp2px(56);
        setMeasuredDimension(preferredSize, preferredSize);
    }

    public void setBackgroundColor(@ColorInt int color) {
        GradientDrawable drawable = (GradientDrawable) getBackground();
        drawable.setColor(color);

        float[] hsv = new float[3];
        Color.RGBToHSV(Color.red(color), Color.green(color), Color.blue(color), hsv);
        hsv[2] *= 0.7f;
        int selectedColor = Color.HSVToColor(hsv);
        ColorStateList tint = createColorStateList(selectedColor, color);
        DrawableCompat.setTintList(drawable, tint);
    }

    private ColorStateList createColorStateList(int selectedColor, int defaultColor) {
        final int[][] states = new int[3][];
        final int[] colors = new int[3];
        int i = 0;

        states[i] = new int[] { android.R.attr.state_focused, android.R.attr.state_enabled };
        colors[i] = selectedColor;
        i++;

        states[i] = new int[] { android.R.attr.state_pressed, android.R.attr.state_enabled };
        colors[i] = selectedColor;
        i++;

        // Default enabled state
        states[i] = new int[0];
        colors[i] = defaultColor;
        i++;

        return new ColorStateList(states, colors);
    }

    public void setAnimatedType(int type) {
        mAnimatedType = type;
    }

    public void show() {
        Logger.d("SimpleFloatingButton show()");
        if (!mIsCreated) {
            ViewGroup rootView = (ViewGroup) mActivity.findViewById(android.R.id.content);
            if (!(rootView instanceof FrameLayout)) {
                throw new RuntimeException("rootView is not FrameLayout?");
            }

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            int paddingSize = dp2px(16);
            params.setMargins(paddingSize, paddingSize, paddingSize, paddingSize);
            params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
            rootView.addView(this, params);
            mIsCreated = true;
            mIsShowed = true;
        } else {
            animate().cancel();
            if (mAnimatedType == ANIMATED_SCALE) {
                ViewCompat.animate(this)
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(200)
                        .setInterpolator(LINEAR_OUT_SLOW_IN_INTERPOLATOR)
                        .setListener(new ViewPropertyAnimatorListenerAdapter() {
                            @Override
                            public void onAnimationStart(View view) {
                                view.setVisibility(View.VISIBLE);
                            }
                        })
                        .start();
            } else {
                ViewCompat.animate(this)
                        .translationY(0)
                        .setInterpolator(FAST_OUT_SLOW_IN_INTERPOLATOR)
                        .setListener(new ViewPropertyAnimatorListenerAdapter() {
                            @Override
                            public void onAnimationStart(View view) {
                                view.setVisibility(View.VISIBLE);
                            }
                        })
                        .start();
            }
            mIsShowed = true;
        }
    }

    public void hide() {
        Logger.d("SimpleFloatingButton hide()");
        if (mAnimatedType == ANIMATED_SCALE) {
            ViewCompat.animate(this)
                    .scaleX(0f)
                    .scaleY(0f)
                    .alpha(0f)
                    .setDuration(200)
                    .setInterpolator(FAST_OUT_LINEAR_IN_INTERPOLATOR)
                    .setListener(new ViewPropertyAnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(View view) {
                            setVisibility(View.INVISIBLE);
                        }
                    })
                    .start();
        } else {
            ViewCompat.animate(this)
                    .translationY(dp2px(72))
                    .setInterpolator(FAST_OUT_SLOW_IN_INTERPOLATOR)
                    .setListener(new ViewPropertyAnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(View view) {
                            setVisibility(View.INVISIBLE);
                        }
                    })
                    .start();
        }
        mIsShowed = false;
    }

    public void destroy() {
        animate().cancel();
        ViewGroup rootView = (ViewGroup) mActivity.findViewById(android.R.id.content);
        rootView.removeView(this);
    }

    public boolean isShowed() {
        return mIsShowed;
    }

    public IActivityTouchEventListener getActivityTouchEventListener() {
        if (mActivityTouchEventListener == null) {
            mActivityTouchEventListener = new ActivityTouchEventListener();
        }
        return mActivityTouchEventListener;
    }

    private class ActivityTouchEventListener implements IActivityTouchEventListener {

        private final int TRIGGER_OFFSET = dp2px(200f);
        private float mLastY = 0f;
        private float mOffsetY = 0f;

        @Override
        public void onDispatch(MotionEvent ev) {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                mLastY = ev.getY();
            } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
                mOffsetY += ev.getY() - mLastY;
                mLastY = ev.getY();
                if (mOffsetY > TRIGGER_OFFSET) {
                    if (!SimpleFloatingButton.this.isShowed()) {
                        SimpleFloatingButton.this.show();
                    }
                } else if (mOffsetY < -TRIGGER_OFFSET) {
                    if (SimpleFloatingButton.this.isShowed()) {
                        SimpleFloatingButton.this.hide();
                    }
                }
            } else if (ev.getAction() == MotionEvent.ACTION_UP) {
                if (mOffsetY > TRIGGER_OFFSET || mOffsetY < -TRIGGER_OFFSET) {
                    mOffsetY = 0f;
                }
            }
        }
    }

    public interface IActivityTouchEventListener {
        void onDispatch(MotionEvent ev);
    }
}
