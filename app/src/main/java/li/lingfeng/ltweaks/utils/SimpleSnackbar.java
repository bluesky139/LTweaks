package li.lingfeng.ltweaks.utils;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Handler;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import static li.lingfeng.ltweaks.utils.ContextUtils.dp2px;

/**
 * Created by smallville on 2017/2/12.
 */

public class SimpleSnackbar extends LinearLayout {

    private static final Interpolator FAST_OUT_SLOW_IN_INTERPOLATOR = new FastOutSlowInInterpolator();
    public static final int LENGTH_LONG  = 3500;
    public static final int LENGTH_SHORT = 2000;

    protected Activity mActivity;
    protected Handler mHandler;

    protected TextView mTextView;
    protected Button mButton;
    protected int mDuration;

    public static SimpleSnackbar make(Activity activity, String mainText, int duration) {
        SimpleSnackbar snackbar = new SimpleSnackbar(activity, mainText, duration);
        return snackbar;
    }

    private SimpleSnackbar(Activity activity, String mainText, int duration) {
        super(activity);
        mActivity = activity;
        mDuration = duration;
        mHandler = new Handler();
        setBackgroundColor(Color.parseColor("#FF303030"));
        setOrientation(LinearLayout.HORIZONTAL);
        createTextView(mainText);
    }

    protected void createTextView(String mainText) {
        mTextView = new TextView(getContext());
        mTextView.setPadding(dp2px(12f), dp2px(14f), dp2px(12f), dp2px(14f));
        mTextView.setTextSize(14f);
        mTextView.setText(mainText);
        mTextView.setTextColor(Color.WHITE);
        mTextView.setEllipsize(TextUtils.TruncateAt.END);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mTextView.setTextAlignment(TextView.TEXT_ALIGNMENT_VIEW_START);
        }

        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT | Gravity.START;
        params.weight = 1;
        addView(mTextView, params);
    }

    public SimpleSnackbar setAction(String buttonText, OnClickListener clickListener) {
        createButton(buttonText, clickListener);
        return this;
    }

    protected void createButton(String buttonText, final OnClickListener clickListener) {
        mButton = new Button(getContext());
        mButton.setMinWidth(dp2px(48f));

        StateListDrawable bgColor = new StateListDrawable();
        bgColor.setExitFadeDuration(250);
        GradientDrawable pressedDrawable = new GradientDrawable();
        pressedDrawable.setColor(Color.parseColor("#FF464646"));
        pressedDrawable.setStroke(dp2px(4f), Color.parseColor("#FF303030"));
        bgColor.addState(new int[] { android.R.attr.state_pressed }, pressedDrawable);
        bgColor.addState(new int[] {}, new ColorDrawable(Color.parseColor("#FF303030")));
        mButton.setBackgroundDrawable(bgColor);

        mButton.setTextColor(Color.parseColor("#FFFF4081"));
        mButton.setText(buttonText);
        mButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.i("SimpleSnackbar onClick.");
                dismiss();
                clickListener.onClick(v);
            }
        });

        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT | Gravity.END;
        params.rightMargin = dp2px(6f);
        addView(mButton, params);
    }

    public void show() {
        Logger.i("SimpleSnackbar show.");
        ViewGroup rootView = (ViewGroup) mActivity.findViewById(android.R.id.content).getRootView();
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM;

        float yPos = rootView.getY() + rootView.getHeight();
        int windowHeight = ViewUtils.getWindowHeight(mActivity);
        Logger.d("yPos " + yPos + ", windowHeight " + windowHeight);
        if (yPos > windowHeight) {
            params.bottomMargin = ViewUtils.getWindowHeightWithNavigator(mActivity) - windowHeight;
            Logger.d("params.bottomMargin " + params.bottomMargin);
        }
        setAlpha(0f);
        rootView.addView(this, params);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ViewCompat.setTranslationY(SimpleSnackbar.this, SimpleSnackbar.this.getHeight());
                ViewCompat.animate(SimpleSnackbar.this)
                        .translationY(0f)
                        .alpha(1f)
                        .setInterpolator(FAST_OUT_SLOW_IN_INTERPOLATOR)
                        .setDuration(250)
                        .start();
            }
        });
        scheduleDismiss();
    }

    public void dismiss() {
        Logger.i("SimpleSnackbar dismiss.");
        mHandler.removeCallbacksAndMessages(null);
        ViewCompat.animate(this)
                .translationY(getHeight())
                .alpha(0f)
                .setInterpolator(FAST_OUT_SLOW_IN_INTERPOLATOR)
                .setDuration(250)
                .setListener(new ViewPropertyAnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(View view) {
                        SimpleSnackbar.this.setVisibility(View.INVISIBLE);
                        ((ViewGroup) SimpleSnackbar.this.getParent()).removeView(SimpleSnackbar.this);
                    }
                })
                .start();
    }

    protected void scheduleDismiss() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dismiss();
            }
        }, mDuration);
    }
}
