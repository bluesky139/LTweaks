package li.lingfeng.ltweaks.xposed.system;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.DrawableRes;

import org.apache.commons.lang3.NotImplementedException;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by sv on 18-2-16.
 */

public abstract class XposedTile extends XposedBase {

    private final String ACTION_UPDATE_STATE = getClass().getName() + ".ACTION_UPDATE_STATE";
    protected final String ACTION_SWITCH = getClass().getName() + ".ACTION_SWITCH";
    protected Context mContext;
    private SwitchReceiver mReceiver;

    @Override
    protected void handleLoadPackage() throws Throwable {
        if (!lpparam.packageName.equals(PackageNames.ANDROID_SYSTEM_UI)) {
            return;
        }
        final Class clsQsTileHost = findClass(ClassNames.QS_TILE_HOST);
        final Class clsIntentTile = findClass(ClassNames.INTENT_TILE);

        String methodOnTuningChanged = "onTuningChanged";
        try {
            XposedHelpers.findMethodExact(clsQsTileHost, "onTuningChanged", String.class, String.class);
        } catch (Throwable e) {
            methodOnTuningChanged = "recreateTiles";
        }

        hookAllMethods(clsQsTileHost, methodOnTuningChanged, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                if (mReceiver == null) {
                    Logger.i("Register " + XposedTile.this.getClass().getSimpleName() + " switch receiver.");
                    mReceiver = new SwitchReceiver();
                    IntentFilter filter = new IntentFilter(ACTION_SWITCH);
                    mContext.registerReceiver(mReceiver, filter);
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                updateTileState(true);
            }
        });

        hookAllMethods(clsQsTileHost, "loadTileSpecs", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("loadTileSpecs return tile " + XposedTile.this.getClass().getSimpleName());
                List<String> tiles = (List<String>) param.getResult();
                tiles.add(XposedTile.this.getClass().getSimpleName());
            }
        });

        findAndHookMethod(clsQsTileHost, "createTile", String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String tileSpec = (String) param.args[0];
                if (tileSpec.equals(XposedTile.this.getClass().getSimpleName())) {
                    Logger.i("Create " + XposedTile.this.getClass().getSimpleName() + " tile.");
                    Object tile = XposedHelpers.callStaticMethod(clsIntentTile, "create", param.thisObject, "intent(" + ACTION_UPDATE_STATE + ")");
                    param.setResult(tile);
                }
            }
        });
    }

    private class SwitchReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, Intent intent) {
            if (!intent.getAction().equals(ACTION_SWITCH)) {
                return;
            }
            final boolean isOn = intent.getBooleanExtra("is_on", true);
            onSwitch(context, isOn);
            updateTileState(!isOn);
        }
    }

    protected abstract String getTileName(boolean isOn);
    protected abstract String getTileDesc();
    protected abstract @DrawableRes int getTileIcon(boolean isOn);
    protected abstract void onSwitch(Context context, boolean isOn);

    @SuppressWarnings("MissingPermission")
    protected void updateTileState(boolean isOn) {
        Logger.i(getClass().getSimpleName() + " updateTileState, is on " + isOn);
        Intent intent = new Intent(ACTION_UPDATE_STATE);
        intent.putExtra("visible", true);
        intent.putExtra("contentDescription", getTileDesc());
        intent.putExtra("label", getTileName(isOn));
        intent.putExtra("iconPackage", PackageNames.L_TWEAKS);
        intent.putExtra("iconId", getTileIcon(isOn));

        Intent clickIntent = new Intent(ACTION_SWITCH);
        clickIntent.putExtra("is_on", isOn);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        intent.putExtra("onClick", pendingIntent);
        mContext.sendBroadcast(intent);

        if (enableNotification()) {
            try {
                NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                int id = getNotificationId();
                if (!isOn) {
                    Notification.Builder builder = new Notification.Builder(mContext)
                            .setSmallIcon(getNotificationIcon())
                            .setWhen(0)
                            .setOngoing(true)
                            .setTicker(getNotificationTitle())
                            .setDefaults(0)
                            .setPriority(Notification.PRIORITY_LOW)
                            .setContentTitle(getNotificationTitle())
                            .setContentText(getNotificationText())
                            .setContentIntent(pendingIntent);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        builder.setVisibility(Notification.VISIBILITY_PUBLIC);
                    }
                    Notification notification = builder.build();
                    notificationManager.notify(id, notification);
                } else {
                    notificationManager.cancel(id);
                }
            } catch (Throwable e) {
                Logger.e("Can't set notification for " + getClass().getSimpleName() + ", " + e);
            }
        }
    }

    protected boolean enableNotification() {
        return false;
    }

    protected int getNotificationId() {
        throw new NotImplementedException("getNotificationId()");
    }

    protected @DrawableRes int getNotificationIcon() {
        throw new NotImplementedException("getNotificationIcon()");
    }

    protected String getNotificationTitle() {
        throw new NotImplementedException("getNotificationTitle()");
    }

    protected String getNotificationText() {
        throw new NotImplementedException("getNotificationText()");
    }
}
