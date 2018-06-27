package li.lingfeng.ltweaks.xposed.system;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.prefs.Prefs;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2018/6/27.
 */
@XposedLoad(packages = PackageNames.SHADOWSOCKS, prefs = {})
public class XposedShadowsocksPrimaryDns extends XposedBase {
    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(JSONObject.class, "put", String.class, Object.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String key = (String) param.args[0];
                if (key.equals("PrimaryDNS")) {
                    String value = Prefs.instance().getString(R.string.key_shadowsocks_primary_dns, "");
                    String[] dnsArray = StringUtils.split(value, ',');
                    if (dnsArray.length == 0) {
                        return;
                    }

                    Logger.i("Modify primary dns, " + value);
                    JSONObject config = (JSONObject) ((JSONArray) param.args[1]).get(0);
                    JSONArray newConfigs = new JSONArray();
                    for (int i =0; i < dnsArray.length; ++i) {
                        JSONObject newConfig = new JSONObject(config, IteratorUtils.toArray(config.keys(), String.class));
                        newConfig.put("Name", "Primary-" + i);
                        newConfig.put("Address", dnsArray[i]);
                        newConfigs.put(newConfig);
                    }
                    param.args[1] = newConfigs;
                }
            }
        });
    }
}
