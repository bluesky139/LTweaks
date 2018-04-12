package li.lingfeng.ltweaks.xposed.system;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.ViewUtils;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2018/4/11.
 */
@XposedLoad(packages = PackageNames.ANDROID_SYSTEM_UI, prefs = R.string.key_quick_settings_brightness_by_volume_buttons)
public class XposedVolumeButtonOnBrightness extends XposedBase {

    private static final String SYSTEM_BAR = Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1 ?
            "com.android.systemui.statusbar.phone.PhoneStatusBar" : "com.android.systemui.statusbar.phone.StatusBar";
    private static final String STATUS_BAR_WINDOW_VIEW = "com.android.systemui.statusbar.phone.StatusBarWindowView";
    private static final String NOTIFICATION_PANEL_VIEW = "com.android.systemui.statusbar.phone.NotificationPanelView";
    private static final float BRIGHTNESS_ADJ_STEPS = 1f / 128f;

    private Object mBrightnessMirrorController;
    private Handler mHandler;
    private int mMinBrightness;
    private int mMaxBrightness;
    private boolean mQsFullyExpanded = false;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(SYSTEM_BAR, "makeStatusBarView", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    mBrightnessMirrorController = XposedHelpers.getObjectField(param.thisObject, "mBrightnessMirrorController");
                } catch (Throwable _) {}
                mHandler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");

                Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                mMinBrightness = (int) XposedHelpers.callMethod(powerManager, "getMinimumScreenBrightnessSetting");
                mMaxBrightness = (int) XposedHelpers.callMethod(powerManager, "getMaximumScreenBrightnessSetting");
            }
        });

        findAndHookMethod(STATUS_BAR_WINDOW_VIEW, "dispatchKeyEvent", KeyEvent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!mQsFullyExpanded) {
                    return;
                }

                KeyEvent keyEvent = (KeyEvent) param.args[0];
                int keyCode = keyEvent.getKeyCode();
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                        Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        boolean automatic = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL) != Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;

                        if (automatic) {
                            float adj = Settings.System.getFloat(context.getContentResolver(), "screen_auto_brightness_adj", 0f);
                            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                                adj -= BRIGHTNESS_ADJ_STEPS;
                                if (adj < -1f) {
                                    adj = -1f;
                                }
                            } else {
                                adj += BRIGHTNESS_ADJ_STEPS;
                                if (adj > 1f) {
                                    adj = 1f;
                                }
                            }
                            Logger.v("Set brightness adj " + adj + " by volume button.");
                            Settings.System.putFloat(context.getContentResolver(), "screen_auto_brightness_adj", adj);

                        } else {
                            int brightness = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, -1);
                            if (brightness > 0) {
                                int oldBrightness = brightness;
                                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                                    if (brightness - 1 >= mMinBrightness) {
                                        --brightness;
                                    }
                                } else {
                                    if (brightness + 1 <= mMaxBrightness) {
                                        ++brightness;
                                    }
                                }
                                if (brightness != oldBrightness) {
                                    Logger.v("Set brightness " + brightness + " by volume button.");
                                    Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightness);
                                }

                                if (mBrightnessMirrorController != null) {
                                    XposedHelpers.callMethod(mBrightnessMirrorController, "showMirror");
                                    ViewGroup statusBarWindow = (ViewGroup) param.thisObject;
                                    ViewGroup qsPanel = (ViewGroup) ViewUtils.findViewByName(statusBarWindow, "quick_settings_panel");
                                    View toggleSlider = ViewUtils.findViewByName(qsPanel, "brightness_slider");
                                    XposedHelpers.callMethod(mBrightnessMirrorController, "setLocation", toggleSlider.getParent());
                                    mHandler.removeCallbacks(mHideMirrorRunnable);
                                    mHandler.postDelayed(mHideMirrorRunnable, 1000);
                                }
                            }
                        }
                    }
                    param.setResult(true);
                }
            }
        });

        findAndHookMethod(NOTIFICATION_PANEL_VIEW, "setQsExpansion", float.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mQsFullyExpanded = XposedHelpers.getBooleanField(param.thisObject, "mQsFullyExpanded");
            }
        });
    }

    private Runnable mHideMirrorRunnable = new Runnable() {
        @Override
        public void run() {
            XposedHelpers.callMethod(mBrightnessMirrorController, "hideMirror");
        }
    };
}
