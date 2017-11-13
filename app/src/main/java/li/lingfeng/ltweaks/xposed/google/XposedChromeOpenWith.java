package li.lingfeng.ltweaks.xposed.google;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.view.MenuItem;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Callback;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;

/**
 * Created by smallville on 2017/11/7.
 */
@XposedLoad(packages = {
        PackageNames.CHROME,
        PackageNames.CHROME_BETA,
        PackageNames.CHROME_DEV,
        PackageNames.CHROME_CANARY,
        PackageNames.ANDROID,
        PackageNames.ANDROID_SYSTEM
}, prefs = R.string.key_chrome_open_with)
public class XposedChromeOpenWith extends XposedChromeBase {

    private static final String CHOOSER_ACTIVITY = "com.android.internal.app.ChooserActivity";
    private static final String RESOLVER_ACTIVITY = "com.android.internal.app.ResolverActivity";
    private static final String EXTRA_AUTO_LAUNCH_SINGLE_CHOICE = "android.intent.extra.AUTO_LAUNCH_SINGLE_CHOICE";
    private static String MENU_OPEN_WITH;

    @Override
    protected void handleLoadPackage() throws Throwable {
        super.handleLoadPackage();
        if (lpparam.packageName.equals(PackageNames.ANDROID)) {
            hookAndroid();
        } else if (lpparam.packageName.equals(PackageNames.ANDROID_SYSTEM)) {
            hookAndroidSystem();
        } else {
            hookAtActivityCreate(new Callback.C1<Activity>() {
                @Override
                public void onResult(Activity activity) {
                    hookChrome();
                }
            });
        }
    }

    private void hookAndroid() {
        hookAllMethods(ClassNames.PACKAGE_MANAGER_SERVICE, "filterCandidatesWithDomainPreferredActivitiesLPr", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Intent intent = (Intent) param.args[0];
                if (intent.getBooleanExtra("ltweaks_activities_without_preferred_filter", false)) {
                    Logger.d("Return whole resolve infos in filterCandidatesWithDomainPreferredActivitiesLPr().");
                    List<ResolveInfo> infos = (List<ResolveInfo>) param.args[2];
                    param.setResult(infos);
                }
            }
        });

        /*if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            hookAllMethods(ClassNames.PACKAGE_MANAGER_SERVICE, "queryIntentActivities", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    removePackageFromActivities(param);
                }
            });
        } else {
            hookAllMethods(ClassNames.PACKAGE_MANAGER_SERVICE, "queryIntentActivitiesInternal", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    removePackageFromActivities(param);
                }
            });
        }*/
    }

    private void removePackageFromActivities(XC_MethodHook.MethodHookParam param) {
        Intent intent = (Intent) param.args[0];
        String packageName = intent.getStringExtra("ltweaks_remove_package");
        if (packageName != null) {
            List<ResolveInfo> infos = (List<ResolveInfo>) param.getResult();
            for (int i = infos.size() - 1; i >= 0; --i) {
                ResolveInfo info = infos.get(i);
                if (info.activityInfo.packageName.equals(packageName)) {
                    Logger.d("Removed " + packageName + " from queryIntentActivities().");
                    infos.remove(i);
                }
            }
        }
    }

    private void hookAndroidSystem() {
        hookAllMethods(RESOLVER_ACTIVITY, "shouldAutoLaunchSingleChoice", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                if (activity.getClass().getName().equals(CHOOSER_ACTIVITY)) {
                    Logger.d("shouldAutoLaunchSingleChoice false");
                    param.setResult(false);
                }
            }
        });
    }

    private void hookChrome() {
        MENU_OPEN_WITH = ContextUtils.getLString(R.string.chrome_open_with);
        newMenu(MENU_OPEN_WITH, 1006, new NewMenuCallback() {
            @Override
            public void onOptionsItemSelected(Activity activity, MenuItem item, String url, boolean isCustomTab) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                intent.putExtra("ltweaks_activities_without_preferred_filter", true);
                intent.putExtra("ltweaks_remove_package", lpparam.packageName);
                Intent chooserIntent = Intent.createChooser(intent, MENU_OPEN_WITH);
                chooserIntent.putExtra(EXTRA_AUTO_LAUNCH_SINGLE_CHOICE, false);
                activity.startActivity(chooserIntent);
            }
        });
    }
}
