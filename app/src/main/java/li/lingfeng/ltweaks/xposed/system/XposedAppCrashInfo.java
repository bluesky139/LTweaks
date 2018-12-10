package li.lingfeng.ltweaks.xposed.system;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.StringBuilderPrinter;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.ViewUtils;
import li.lingfeng.ltweaks.xposed.XposedCommon;

@XposedLoad(packages = {}, prefs = R.string.key_debug_app_crash_info)
public class XposedAppCrashInfo extends XposedCommon {

    private static final String APP_ERRORS = "com.android.server.am.AppErrors";
    private static final String APP_ERROR_DIALOG = "com.android.server.am.AppErrorDialog";
    private static final String APP_ERROR_DIALOG_DATA = APP_ERROR_DIALOG + "$Data";

    private Object mCrashInfo;

    @Override
    protected void handleLoadPackage() throws Throwable {
        if (lpparam.packageName.equals(PackageNames.ANDROID)) {
            hookAndroid();
        } else if (isUserInstalledApp()) {
            hookUserInstalledApp();
        }
    }

    private void hookAndroid() {
        hookAllMethods(APP_ERRORS, "crashApplicationInner", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mCrashInfo = param.args[1];
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mCrashInfo = null;
            }
        });

        findAndHookConstructor(APP_ERROR_DIALOG_DATA, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mCrashInfo == null) {
                    Logger.w("No crash info when constructing error dialog data.");
                    return;
                }
                Logger.d("Attach crash info to error dialog data.");
                XposedHelpers.setAdditionalInstanceField(param.thisObject, "crashInfo", mCrashInfo);
                mCrashInfo = null;
            }
        });

        findAndHookConstructor(APP_ERROR_DIALOG, Context.class, findClass(ClassNames.ACTIVITY_MANAGER_SERVICE), findClass(APP_ERROR_DIALOG_DATA), new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mCrashInfo = XposedHelpers.getAdditionalInstanceField(param.args[2], "crashInfo");
                if (mCrashInfo == null) {
                    Logger.w("No crash info when constructing error dialog.");
                }
            }
        });

        findAndHookMethod(APP_ERROR_DIALOG, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Logger.d("Append crash info button.");
                final Dialog dialog = (Dialog) param.thisObject;
                FrameLayout frame = dialog.findViewById(android.R.id.custom);
                final LinearLayout linearLayout = ViewUtils.findViewByType(frame, LinearLayout.class);
                final Button button = new Button(dialog.getContext());
                button.setText("Show crash info");
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showCrashInfo(dialog, linearLayout);
                        linearLayout.removeView(button);
                    }
                });
                linearLayout.addView(button);
            }
        });

        findAndHookMethod(APP_ERROR_DIALOG, "onStop", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mCrashInfo = null;
            }
        });
    }

    private void showCrashInfo(Dialog dialog, LinearLayout linearLayout) {
        if (mCrashInfo == null) {
            Toast.makeText(dialog.getContext(), "No crash info.", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder stringBuilder = new StringBuilder();
        StringBuilderPrinter printer = new StringBuilderPrinter(stringBuilder);
        XposedHelpers.callMethod(mCrashInfo, "dump", printer, "");

        TextView textView = new TextView(dialog.getContext());
        textView.setText(stringBuilder.toString());
        ScrollView scrollView = new ScrollView(dialog.getContext());
        scrollView.addView(textView);
        linearLayout.addView(scrollView);
        mCrashInfo = null;
    }

    private void hookUserInstalledApp() {
        findAndHookMethod(Thread.class, "setDefaultUncaughtExceptionHandler", Thread.UncaughtExceptionHandler.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.d("Ignore default exception handler " + param.args[0].getClass().getName());
                param.setResult(null);
            }
        });

        findAndHookMethod(Thread.class, "setUncaughtExceptionHandler", Thread.UncaughtExceptionHandler.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.d("Ignore exception handler " + param.args[0].getClass().getName());
                param.setResult(null);
            }
        });
    }
}
