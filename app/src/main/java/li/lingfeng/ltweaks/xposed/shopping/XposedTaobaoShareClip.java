package li.lingfeng.ltweaks.xposed.shopping;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;

/**
 * Created by smallville on 2017/2/18.
 */
@XposedLoad(packages = PackageNames.TAOBAO, prefs = R.string.key_taobao_share_item)
public class XposedTaobaoShareClip extends XposedShareClip {

    private static final String ITEM_ACTIVITY = "com.taobao.tao.detail.activity.DetailActivity";

    @Override
    protected String getItemActivity() {
        return ITEM_ACTIVITY;
    }
}
