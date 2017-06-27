package li.lingfeng.ltweaks.xposed.system;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.Shell;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/6/26.
 */
@XposedLoad(packages = PackageNames.ANDROID_SYSTEM_UI, prefs = R.string.key_quick_settings_tile_adb_wireless)
public class XposedAdbWireless extends XposedBase {

    private static final String ACTION_UPDATE_STATE = XposedAdbWireless.class + ".ACTION_UPDATE_STATE";
    private static final String ACTION_ADB_SWITCH = XposedAdbWireless.class + ".ACTION_ADB_SWITCH";
    private Context mContext;
    private AdbSwitchReceiver mReceiver;

    @Override
    protected void handleLoadPackage() throws Throwable {
        final Class clsQsTileHost = findClass(ClassNames.QS_TILE_HOST);
        final Class clsIntentTile = findClass(ClassNames.INTENT_TILE);

        findAndHookMethod(clsQsTileHost, "onTuningChanged", String.class, String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                if (mReceiver == null) {
                    Logger.i("Register adb switch receiver.");
                    mReceiver = new AdbSwitchReceiver();
                    IntentFilter filter = new IntentFilter();
                    filter.addAction(ACTION_ADB_SWITCH);
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
                Logger.i("loadTileSpecs return one more tile adb_wireless");
                List<String> tiles = (List<String>) param.getResult();
                tiles.add("adb_wireless");
            }
        });

        findAndHookMethod(clsQsTileHost, "createTile", String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String tileSpec = (String) param.args[0];
                if (tileSpec.equals("adb_wireless")) {
                    Logger.i("Create adb_wireless tile.");
                    Object tile = XposedHelpers.callStaticMethod(clsIntentTile, "create", param.thisObject, "intent(" + ACTION_UPDATE_STATE + ")");
                    param.setResult(tile);
                }
            }
        });
    }

    private class AdbSwitchReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals(ACTION_ADB_SWITCH)) {
                return;
            }
            boolean isWireless = intent.getBooleanExtra("is_wireless", true);
            try {
                runCmd(isWireless);
                updateTileState(!isWireless);
            } catch (Exception e) {
                Toast.makeText(context, "Failed to switch adb, no root?", Toast.LENGTH_SHORT).show();
            }
        }

        private void runCmd(boolean isWireless) throws Exception {
            Shell shell = new Shell("su");
            shell.run("setprop service.adb.tcp.port " + (isWireless ? "5555" : "-1"));
            shell.run("stop adbd");
            shell.run("start adbd");
            shell.close();
        }
    }

    private void updateTileState(boolean isWireless) {
        Logger.i("AdbWireless updateTileState");
        Intent intent = new Intent(ACTION_UPDATE_STATE);
        intent.putExtra("visible", true);
        intent.putExtra("contentDescription", "Use \"adb connect x:x:x:x:5555\" to connect.");
        intent.putExtra("label", "Adb Wireless");
        intent.putExtra("iconPackage", PackageNames.L_TWEAKS);
        if (isWireless)
            intent.putExtra("iconId", R.drawable.ic_google);
        else
            intent.putExtra("iconId", R.drawable.ic_communication);

        Intent clickIntent = new Intent(ACTION_ADB_SWITCH);
        clickIntent.putExtra("is_wireless", isWireless);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        intent.putExtra("onClick", pendingIntent);
        mContext.sendBroadcast(intent);
    }
}
