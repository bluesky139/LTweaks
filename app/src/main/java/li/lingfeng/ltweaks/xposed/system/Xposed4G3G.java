package li.lingfeng.ltweaks.xposed.system;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.prefs.Prefs;
import li.lingfeng.ltweaks.utils.Logger;

/**
 * Created by sv on 18-2-16.
 */
@XposedLoad(packages = {
        PackageNames.ANDROID_SYSTEM_UI,
        PackageNames.ANDROID_PHONE }, prefs = R.string.key_quick_settings_tile_4g3g)
public class Xposed4G3G extends XposedTile {

    private static final String PHONE_FACTORY = "com.android.internal.telephony.PhoneFactory";
    private SwitchHandler mHandler;
    private SwitchNetTypeReceiver mSwitchNetTypeReceiver;

    @Override
    protected void handleLoadPackage() throws Throwable {
        super.handleLoadPackage();
        if (lpparam.packageName.equals(PackageNames.ANDROID_PHONE)) {
            handleLoadAndroidPhone();
        }
    }

    private void handleLoadAndroidPhone() {
        findAndHookMethod(PHONE_FACTORY, "makeDefaultPhone", Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Context context = (Context) param.args[0];
                initAndroidPhone(context);
            }
        });
    }

    private void initAndroidPhone(Context context) {
        Logger.i("Init android phone for Xposed4G3G.");
        Prefs.instance().registerPreferenceChangeKey(R.string.key_quick_settings_tile_4g);
        Prefs.instance().registerPreferenceChangeKey(R.string.key_quick_settings_tile_3g);
        mHandler = new SwitchHandler();
        mSwitchNetTypeReceiver = new SwitchNetTypeReceiver();
        IntentFilter filter = new IntentFilter(ACTION_SWITCH);
        context.registerReceiver(mSwitchNetTypeReceiver, filter);
    }

    private class SwitchNetTypeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                boolean isOn = intent.getBooleanExtra("is_on", true);
                int typeKey = isOn ? R.string.key_quick_settings_tile_3g : R.string.key_quick_settings_tile_4g;
                String strType = Prefs.instance().getString(typeKey, null);
                if (strType == null) {
                    Logger.e("SwitchNetTypeReceiver strType is null.");
                    return;
                }
                int type = Integer.parseInt(strType);
                Logger.d("SwitchNetTypeReceiver setPreferredNetworkType " + type);
                Class cls = findClass(PHONE_FACTORY);
                Object phone = XposedHelpers.callStaticMethod(cls, "getDefaultPhone");
                Message msg = mHandler.obtainMessage(0);
                XposedHelpers.callMethod(phone, "setPreferredNetworkType", type, msg);
            } catch (Throwable e) {
                Logger.e("SwitchNetTypeReceiver error, " + e);
                Logger.stackTrace(e);
            }
        }
    }

    private static class SwitchHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            try {
                Object e = XposedHelpers.getObjectField(msg.obj, "exception");
                if (e != null) {
                    Logger.e("Set preferred net type failed, " + e);
                }
            } catch (Throwable e) {
                Logger.w("Don't know preferred net type is set or not, " + e);
            }
        }
    }

    @Override
    protected int getPriority() {
        return 1;
    }

    @Override
    protected String getTileName(boolean isOn) {
        return "4G/3G";
    }

    @Override
    protected String getTileDesc() {
        return "4G/3G switch";
    }

    @Override
    protected int getTileIcon(boolean isOn) {
        return isOn ? R.drawable.ic_4g : R.drawable.ic_3g;
    }

    @Override
    protected void onSwitch(Context context, boolean isOn) throws Throwable {
    }

    @Override
    protected void onLongClick(Context context) throws Throwable {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setComponent(new ComponentName(PackageNames.ANDROID_SETTINGS, ClassNames.RADIO_INFO));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        collapseStatusBar();
    }
}
