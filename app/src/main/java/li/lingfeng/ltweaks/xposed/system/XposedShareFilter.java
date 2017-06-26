package li.lingfeng.ltweaks.xposed.system;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Build;
import android.os.ServiceManager;
import android.support.annotation.RequiresApi;
import android.util.ArrayMap;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.IntentActions;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.prefs.Prefs;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/6/24.
 */
@XposedLoad(packages = PackageNames.ANDROID, prefs = {})
public class XposedShareFilter extends XposedBase {

    private Context mContext;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(ClassNames.ACTIVITY_MANAGER_SERVICE, "finishBooting", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Field field = param.thisObject.getClass().getDeclaredField("mContext");
                field.setAccessible(true);
                mContext = (Context) field.get(param.thisObject);
            }
        });

        hookAllMethods(ClassNames.ACTIVITY_INTENT_RESOLVER, "queryIntent", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Intent intent = (Intent) param.args[0];
                if (!IntentActions.sSendActions.contains(intent.getAction()) || mContext == null) {
                    return;
                }
                int uid = Binder.getCallingUid();
                ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(PackageNames.L_TWEAKS, 0);
                if (uid == appInfo.uid) {
                    return;
                }

                Set<String> activities = Prefs.instance().getStringSet(R.string.key_system_share_filter_activities, null);
                if (activities == null || activities.isEmpty()) {
                    return;
                }

                List<ResolveInfo> results = (List<ResolveInfo>) param.getResult();
                int removedCount = 0;
                for (int i = results.size() - 1; i >= 0; --i) {
                    ResolveInfo info = results.get(i);
                    if (activities.contains(info.activityInfo.applicationInfo.packageName + "/" + info.activityInfo.name)) {
                        results.remove(i);
                        ++removedCount;
                    }
                }
                Logger.i("Removed " + removedCount + " share activities for " + intent.getAction());
            }
        });
    }
}
