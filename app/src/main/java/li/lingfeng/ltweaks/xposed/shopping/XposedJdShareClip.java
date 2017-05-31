package li.lingfeng.ltweaks.xposed.shopping;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;

/**
 * Created by smallville on 2017/5/31.
 */
@XposedLoad(packages = PackageNames.JD, prefs = R.string.key_jd_share_item)
public class XposedJdShareClip extends XposedShareClip {

    private static final String ITEM_ACTIVITY = "com.jd.lib.productdetail.ProductDetailActivity";
    private static final String SHARE_ACTIVITY = "com.jingdong.app.mall.basic.ShareActivity";

    @Override
    protected String getItemActivity() {
        return ITEM_ACTIVITY;
    }

    @Override
    protected String getShareActivity() {
        return SHARE_ACTIVITY;
    }
}
