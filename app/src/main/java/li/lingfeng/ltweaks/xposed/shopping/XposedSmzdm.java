package li.lingfeng.ltweaks.xposed.shopping;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import de.robv.android.xposed.XC_MethodHook;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2016/12/19.
 */
@XposedLoad(packages = PackageNames.SMZDM, prefs = R.string.key_smzdm_open_link_in_jd_app)
public class XposedSmzdm extends XposedBase {

    @Override
    public void handleLoadPackage() throws Throwable {
        findAndHookMethod(Activity.class, "startActivity", Intent.class, Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Intent intent = (Intent) param.args[0];
                Logger.intent(intent);
                if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null
                        && "openapp.jdmobile".equals(intent.getData().getScheme())
                        && !intent.getBooleanExtra("from_ltweaks", false)) {
                    Activity activity = (Activity) param.thisObject;
                    String params = intent.getData().getQueryParameter("params");
                    JSONObject jParams = (JSONObject) JSON.parse(params);
                    String itemId = jParams.getString("skuId");
                    String url = jParams.getString("url");
                    if (itemId != null) {
                        openJD(activity, itemId);
                        param.setResult(null);
                    } else if (url != null) {
                        openJDUrl(activity, url);
                        param.setResult(null);
                    }
                }
            }
        });
    }

    private void openJD(Activity activity, String itemId) {
        Logger.i("Start JD with item id " + itemId);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("openapp.jdmobile://virtual?params={\"category\":\"jump\",\"des\":\"productDetail\",\"skuId\":\"" + itemId + "\",\"sourceType\":\"Item\",\"sourceValue\":\"view-ware\"}"));
        intent.putExtra("from_ltweaks", true);
        activity.startActivity(intent);
    }

    private void openJDUrl(Activity activity, String url) {
        Logger.i("Start JD url " + url);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("openapp.jdmobile://virtual?params={\"category\":\"jump\",\"des\":\"m\",\"url\":\"" + url + "\"}"));
        intent.putExtra("from_ltweaks", true);
        activity.startActivity(intent);
    }
}
