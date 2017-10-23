package li.lingfeng.ltweaks.xposed.system;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.prefs.PrefKeys;
import li.lingfeng.ltweaks.prefs.Prefs;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

import static li.lingfeng.ltweaks.prefs.SharedPreferences.ACTION_PREF_CHANGE_PREFIX;

/**
 * Created by smallville on 2017/10/12.
 */
@XposedLoad(packages = PackageNames.ANDROID, prefs = R.string.key_lineage_os_live_display_time)
public class XposedLiveDisplayTwilight extends XposedBase {

    private static final String TWILIGHT_TRACKER = "org.cyanogenmod.platform.internal.display.TwilightTracker";
    private static final String LOCATION_HANDLER = TWILIGHT_TRACKER + "$LocationHandler";
    private static final String TWILIGHT_STATE   = TWILIGHT_TRACKER + "$TwilightState";
    private static final String ACTION_UPDATE_TWILIGHT_STATE = XposedLiveDisplayTwilight.class.getName() + ".ACTION_UPDATE_TWILIGHT_STATE";
    private Object mTwilightTracker;
    private String mSunrise;
    private String mSunset;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(LOCATION_HANDLER, "setLocation", Location.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("Prevent " + param.method);
                param.setResult(null);
            }
        });

        findAndHookMethod(LOCATION_HANDLER, "enableLocationUpdates", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(null);
                Logger.i("Prevent " + param.method);
            }
        });

        findAndHookMethod(LOCATION_HANDLER, "updateTwilightState", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(null);
                Logger.i("Prevent " + param.method);
            }
        });

        hookAllMethods(TWILIGHT_TRACKER, "registerListener", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ArrayList listeners = (ArrayList) XposedHelpers.getObjectField(param.thisObject, "mListeners");
                if (listeners.size() == 1) {
                    mTwilightTracker = param.thisObject;
                    mSunrise = Prefs.instance().getString(R.string.key_lineage_os_live_display_time_sunrise, "08:00");
                    mSunset = Prefs.instance().getString(R.string.key_lineage_os_live_display_time_sunset, "19:00");

                    Context context = (Context) XposedHelpers.getObjectField(mTwilightTracker, "mContext");
                    IntentFilter filter = new IntentFilter(ACTION_UPDATE_TWILIGHT_STATE);
                    filter.addAction(Intent.ACTION_TIME_CHANGED);
                    filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
                    filter.addAction(ACTION_PREF_CHANGE_PREFIX + PrefKeys.getById(R.string.key_lineage_os_live_display_time_sunrise));
                    filter.addAction(ACTION_PREF_CHANGE_PREFIX + PrefKeys.getById(R.string.key_lineage_os_live_display_time_sunset));
                    context.registerReceiver(mReceiver, filter);
                    updateTwilightState();
                }
            }
        });
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (intent.getStringExtra("key").equals(PrefKeys.getById(R.string.key_lineage_os_live_display_time_sunrise))) {
                    mSunrise = intent.getStringExtra("value");
                } else {
                    mSunset = intent.getStringExtra("value");
                }
                updateTwilightState();
            } catch (Throwable e) {
                Logger.e("updateTwilightState exception in receiver, " + e);
                Logger.stackTrace(e);
            }
        }
    };

    private void updateTwilightState() throws Throwable {
        TimeZone timeZone = TimeZone.getDefault();
        Logger.i("updateTwilightState sunrise " + mSunrise + ", sunset " + mSunset + ", timeZone "
                + timeZone.getDisplayName(false, TimeZone.LONG) + " "
                + TimeUnit.HOURS.convert(timeZone.getRawOffset(), TimeUnit.MILLISECONDS));
        if (mSunrise.equals(mSunset)) {
            Logger.w("Sunrise equals sunset.");
            return;
        }

        int sunriseHour = Integer.parseInt(mSunrise.split(":")[0]);
        int sunriseMinute = Integer.parseInt(mSunrise.split(":")[1]);
        int sunsetHour = Integer.parseInt(mSunset.split(":")[0]);
        int sunsetMinute = Integer.parseInt(mSunset.split(":")[1]);

        long now = System.currentTimeMillis();
        long todaySunrise = now - now % 86400000 + sunriseHour * 3600000 + sunriseMinute * 60000 - timeZone.getRawOffset();
        long todaySunset = now - now % 86400000 + sunsetHour * 3600000 + sunsetMinute * 60000 - timeZone.getRawOffset();
        long yesterdaySunset = todaySunset - 86400000;
        long tomorrowSunrise = todaySunrise + 86400000;
        boolean isNight = now < todaySunrise || now >= todaySunset;

        Logger.i("setTwilightState isNight " + isNight + ", yesterdaySunset " + yesterdaySunset
                + ", todaySunrise " + todaySunrise + ", todaySunset " + todaySunset
                + ", tomorrowSunrise " + tomorrowSunrise);
        Constructor constructor = XposedHelpers.findConstructorExact(findClass(TWILIGHT_STATE),
                boolean.class, long.class, long.class, long.class, long.class);
        Object state = constructor.newInstance(isNight, yesterdaySunset, todaySunrise, todaySunset, tomorrowSunrise);
        XposedHelpers.callMethod(mTwilightTracker, "setTwilightState", state);

        long nextUpdate = now < todaySunrise ? todaySunrise : (now < todaySunset ? todaySunset : tomorrowSunrise);
        Logger.i("nextUpdate " + nextUpdate);
        Intent intent = new Intent(ACTION_UPDATE_TWILIGHT_STATE);
        Context context = (Context) XposedHelpers.getObjectField(mTwilightTracker, "mContext");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) XposedHelpers.getObjectField(mTwilightTracker, "mAlarmManager");
        alarmManager.cancel(pendingIntent);
        alarmManager.set(AlarmManager.RTC, nextUpdate, pendingIntent);
    }
}
