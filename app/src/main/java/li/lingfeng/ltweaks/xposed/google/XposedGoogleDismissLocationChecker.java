package li.lingfeng.ltweaks.xposed.google;

import android.app.Activity;
import android.os.Bundle;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/6/3.
 */
@XposedLoad(packages = PackageNames.GMS, prefs = R.string.key_google_dismiss_location_checker)
public class XposedGoogleDismissLocationChecker extends XposedBase {

    private static final String LOCATION_CHECKER_ACTIVITY = "com.google.android.location.settings.LocationSettingsCheckerActivity";

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookActivity(LOCATION_CHECKER_ACTIVITY, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("Dismiss gms location settings checker.");
                ((Activity) param.thisObject).finish();
            }
        });
    }
}
