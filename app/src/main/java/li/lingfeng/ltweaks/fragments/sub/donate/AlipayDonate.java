package li.lingfeng.ltweaks.fragments.sub.donate;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.widget.Toast;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.utils.Logger;

/**
 * Created by lilingfeng on 2018/1/18.
 */

public class AlipayDonate {

    private static final String INTENT_URL_FORMAT = "intent://platformapi/startapp?saId=10000007&" +
            "clientVersion=3.7.0.0718&qrcode=https%3A%2F%2Fqr.alipay.com%2FFKX04627JKYY7PCL2BFOFF%3F_s" +
            "%3Dweb-other&_t=1472443966571#Intent;" +
            "scheme=alipayqr;package=com.eg.android.AlipayGphone;end";

    public static void donate(Activity activity) {
        Logger.i("Donate with Alipay.");
        try {
            Intent intent = Intent.parseUri(
                    INTENT_URL_FORMAT,
                    Intent.URI_INTENT_SCHEME
            );
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.alipay_not_installed, Toast.LENGTH_SHORT).show();
        } catch (Throwable e) {
            Toast.makeText(activity, R.string.error, Toast.LENGTH_SHORT).show();
            Logger.e("Can't open alipay, " + e);
        }
    }
}
