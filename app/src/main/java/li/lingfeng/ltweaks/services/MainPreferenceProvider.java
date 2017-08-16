package li.lingfeng.ltweaks.services;

import com.crossbowffs.remotepreferences.RemotePreferenceProvider;

/**
 * Created by lilingfeng on 2017/8/16.
 */

public class MainPreferenceProvider extends RemotePreferenceProvider {

    public MainPreferenceProvider() {
        super("li.lingfeng.ltweaks.mainpreferences", new String[] { "li.lingfeng.ltweaks_preferences" });
    }

    @Override
    protected boolean checkAccess(String prefName, String prefKey, boolean write) {
        return !write;
    }
}
