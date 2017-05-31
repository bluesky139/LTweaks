package li.lingfeng.ltweaks.xposed.shopping;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;

/**
 * Created by smallville on 2017/2/11.
 */
@XposedLoad(packages = PackageNames.SUNING, prefs = R.string.key_suning_share_item)
public class XposedSuningShareClip extends XposedShareClip {

    private static final String ITEM_ACTIVITY = "com.suning.mobile.ebuy.commodity.newgoodsdetail.NewGoodsDetailActivity";
    private static final String SHARE_ACTIVITY = "com.suning.mobile.ebuy.base.host.share.main.ShareActivity";

    @Override
    protected String getItemActivity() {
        return ITEM_ACTIVITY;
    }

    @Override
    protected String getShareActivity() {
        return SHARE_ACTIVITY;
    }
}
