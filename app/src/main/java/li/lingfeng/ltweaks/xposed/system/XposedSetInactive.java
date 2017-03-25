package li.lingfeng.ltweaks.xposed.system;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.usage.IUsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.ServiceManager;
import android.support.annotation.RequiresApi;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.IOUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/3/25.
 */
@XposedLoad(packages = PackageNames.ANDROID, prefs = R.string.key_prevent_running_set_inactive)
public class XposedSetInactive extends XposedBase {
    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(ClassNames.ACTIVITY_MANAGER_SERVICE, "finishBooting", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                RegisterReceiver(param.thisObject);
            }
        });
    }

    private void RegisterReceiver(Object activityManagerService) throws Throwable {
        Field field = activityManagerService.getClass().getDeclaredField("mContext");
        field.setAccessible(true);
        Context context = (Context) field.get(activityManagerService);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(ScreenOffReceiver.ACTION_SET_INACTIVE);
        context.registerReceiver(new ScreenOffReceiver(context), filter);
        Logger.i("ScreenOffReceiver is registered.");
    }

    private static class ScreenOffReceiver extends BroadcastReceiver {

        static final String ACTION_SET_INACTIVE = ScreenOffReceiver.class.getName() + ".ACTION_SET_INACTIVE";

        private IUsageStatsManager mUsageStatsManager;
        private Object mUsageStatsService;
        private Method mMethodSetAppIdle;
        private List<String> mPreventList;

        private AlarmManager mAlarmManager;
        private PendingIntent mSetInactiveIntent;

        ScreenOffReceiver(Context context) throws Exception {
            mUsageStatsManager = IUsageStatsManager.Stub.asInterface(ServiceManager.getService(Context.USAGE_STATS_SERVICE));
            Field field = mUsageStatsManager.getClass().getDeclaredField("this$0");
            field.setAccessible(true);
            mUsageStatsService = field.get(mUsageStatsManager);
            mMethodSetAppIdle = mUsageStatsService.getClass().getDeclaredMethod("setAppIdle", String.class, boolean.class, int.class);
            mMethodSetAppIdle.setAccessible(true);
            mPreventList = IOUtils.readLines("/data/system/me.piebridge.prevent.list");

            mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent();
            intent.setAction(ACTION_SET_INACTIVE);
            mSetInactiveIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                mAlarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 300000, mSetInactiveIntent);
                Logger.i("Set delay to set-inactive, mPreventList size " + mPreventList.size());
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                mAlarmManager.cancel(mSetInactiveIntent);
                Logger.i("Cancel delay to set-inactive.");
            } else if (intent.getAction().equals(ACTION_SET_INACTIVE)) {
                onAlarm();
            }
        }

        private void onAlarm() {
            Logger.d("set-inactive onAlarm");
            for (String name : mPreventList) {
                try {
                    boolean isInactive = mUsageStatsManager.isAppInactive(name, 0);
                    if (isInactive)
                        continue;
                    mMethodSetAppIdle.invoke(mUsageStatsService, name, true, 0);
                    Logger.i("set-inactive " + name);
                } catch (Exception e) {
                    Logger.e("Failed to set-inactive, " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}
