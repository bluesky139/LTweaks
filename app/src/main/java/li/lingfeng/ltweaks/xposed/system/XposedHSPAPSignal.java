package li.lingfeng.ltweaks.xposed.system;

import android.content.res.XModuleResources;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.SparseArray;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.ResLoad;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.Xposed;
import li.lingfeng.ltweaks.xposed.XposedBase;

@XposedLoad(packages = PackageNames.ANDROID_SYSTEM_UI, prefs = R.string.key_display_hspap_signal)
@ResLoad(packages = PackageNames.ANDROID_SYSTEM_UI, prefs = R.string.key_display_hspap_signal)
public class XposedHSPAPSignal extends XposedBase implements IXposedHookInitPackageResources {

    private static final String MOBILE_SIGNAL_CONTROLLER = "com.android.systemui.statusbar.policy.MobileSignalController";
    private static final String MOBILE_ICON_GROUP = MOBILE_SIGNAL_CONTROLLER + "$MobileIconGroup";
    private static final String TELEPHONY_ICONS = "com.android.systemui.statusbar.policy.TelephonyIcons";
    private static final String ACCESSIBILITY_CONTENT_DESCRIPTIONS = "com.android.systemui.statusbar.policy.AccessibilityContentDescriptions";

    private int mIconHP;
    private int mQsDataHP;
    private Object mHP;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(MOBILE_SIGNAL_CONTROLLER, "mapIconSets", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                SparseArray icons = (SparseArray) XposedHelpers.getObjectField(param.thisObject, "mNetworkToIconLookup");
                if (mHP == null) {
                    int[] signalStrength = (int[]) XposedHelpers.getStaticObjectField(findClass(ACCESSIBILITY_CONTENT_DESCRIPTIONS), "PHONE_SIGNAL_STRENGTH");
                    mHP = XposedHelpers.newInstance(findClass(MOBILE_ICON_GROUP),
                            "HP",
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? null : XposedHelpers.getStaticObjectField(findClass(TELEPHONY_ICONS), "TELEPHONY_SIGNAL_STRENGTH"),
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? null : XposedHelpers.getStaticObjectField(findClass(TELEPHONY_ICONS), "QS_TELEPHONY_SIGNAL_STRENGTH"),
                            signalStrength,
                            0, 0,
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? 0 : ContextUtils.getDrawableId("stat_sys_signal_null"),
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? 0 : ContextUtils.getDrawableId("ic_qs_signal_no_signal"),
                            signalStrength[0],
                            ContextUtils.getStringId("accessibility_data_connection_3_5g"),
                            mIconHP,
                            false,
                            mQsDataHP
                    );
                }
                icons.put(TelephonyManager.NETWORK_TYPE_HSPAP, mHP);
                Logger.i("MobileSignalController.mapIconSets H+");
            }
        });
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        XModuleResources modRes = XModuleResources.createInstance(Xposed.MODULE_PATH, resparam.res);
        mQsDataHP = resparam.res.addResource(modRes, R.drawable.ic_qs_signal_hp);
        mIconHP = resparam.res.addResource(modRes, R.drawable.stat_sys_data_fully_connected_hp);
    }
}
