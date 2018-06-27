package li.lingfeng.ltweaks.xposed.system;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.PackageUtils;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/12/25.
 */
@XposedLoad(packages = { PackageNames.ANDROID_SYSTEM_UI, PackageNames.ANDROID }, prefs = R.string.key_recent_task_swipe_to_kill)
public class XposedRecentTaskSwipeToKill extends XposedBase {

    private static final String SWIPE_HELPER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
            "com.android.systemui.SwipeHelper" : "com.android.systemui.recents.views.SwipeHelper";
    private static final String TASK_VIEW = "com.android.systemui.recents.views.TaskView";
    private static final String SLIM_RECENT_PANEL_VIEW = "com.android.systemui.slimrecent.RecentPanelView";
    private XC_MethodHook.Unhook mSlimItemTouchHelperHook;

    @Override
    protected void handleLoadPackage() throws Throwable {
        if (lpparam.packageName.equals(PackageNames.ANDROID)) {
            findAndHookMethod(ClassNames.PACKAGE_MANAGER_SERVICE, "checkUidPermission", String.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String permissionName = (String) param.args[0];
                    if (permissionName.equals("android.permission.FORCE_STOP_PACKAGES")) {
                        String packageName = ((String[]) XposedHelpers.callMethod(param.thisObject, "getPackagesForUid", (int) param.args[1]))[0];
                        if (packageName.equals(PackageNames.ANDROID_SYSTEM_UI)) {
                            Logger.i("Grant permission " + permissionName + " for " + packageName);
                            param.setResult(PackageManager.PERMISSION_GRANTED);
                        }
                    }
                }
            });
            return;
        }

        handleAOSP();
        handleSlim();
    }

    private void handleAOSP() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            findAndHookMethod(SWIPE_HELPER, "dismissChild", View.class, float.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    hookDismissChild(param);
                }
            });
        } else {
            findAndHookMethod(SWIPE_HELPER, "dismissChild", View.class, float.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    hookDismissChild(param);
                }
            });
        }
    }

    private void hookDismissChild(XC_MethodHook.MethodHookParam param) throws Throwable {
        float transaction = (float) XposedHelpers.callMethod(param.thisObject, "getTranslation", param.args[0]);
        if (transaction < 0) {
            return;
        }

        View taskView = (View) param.args[0];
        if (!taskView.getClass().getName().equals(TASK_VIEW)) {
            return;
        }

        Logger.i("Swipe right on recent task to kill.");
        Object task = XposedHelpers.callMethod(taskView, "getTask");
        Object taskKey = XposedHelpers.getObjectField(task, "key");
        Intent intent = (Intent) XposedHelpers.getObjectField(taskKey, "baseIntent");
        String packageName = intent.getComponent().getPackageName();
        PackageUtils.killPackage(taskView.getContext(), packageName);
    }

    private void handleSlim() {
        try {
            findClass(SLIM_RECENT_PANEL_VIEW);
        } catch (Throwable e) {
            return;
        }

        mSlimItemTouchHelperHook = findAndHookConstructor(ClassNames.ITEM_TOUCH_HELPER, ClassNames.ITEM_TOUCH_HELPER_CALLBACK, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (mSlimItemTouchHelperHook == null) {
                    return;
                }
                Class cls = param.args[0].getClass();
                if (cls.getName().startsWith(SLIM_RECENT_PANEL_VIEW)) {
                    mSlimItemTouchHelperHook = null;
                    hookSlimTouchHelper(cls);
                }
            }
        });
    }

    private void hookSlimTouchHelper(Class cls) {
        Logger.d("hookSlimTouchHelper " + cls);
        findAndHookMethod(cls, "onSwiped", findClass(ClassNames.RECYCLER_VIEW_HOLDER), int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                int direction = (int) param.args[1];
                if (direction == 32) {
                    Logger.i("Slim recent swipe right to kill.");
                    int pos = (int) XposedHelpers.callMethod(param.args[0], "getAdapterPosition");
                    Object panelView = XposedHelpers.getSurroundingThis(param.thisObject);
                    Object cardAdapter = XposedHelpers.getObjectField(panelView, "mCardAdapter");
                    Object card = XposedHelpers.callMethod(cardAdapter, "getCard", pos);
                    Object task = XposedHelpers.getObjectField(card, "task");
                    String packageName = (String) XposedHelpers.getObjectField(task, "packageName");
                    Context context = (Context) XposedHelpers.getObjectField(panelView, "mContext");
                    PackageUtils.killPackage(context, packageName);
                }
            }
        });
    }
}
