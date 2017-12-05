package li.lingfeng.ltweaks.xposed.google;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.Html;
import android.view.MenuItem;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Locale;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.ThreadUtils;
import li.lingfeng.ltweaks.utils.ViewUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by lilingfeng on 2017/12/5.
 */
@XposedLoad(packages = {
        PackageNames.CHROME,
        PackageNames.CHROME_BETA,
        PackageNames.CHROME_DEV,
        PackageNames.CHROME_CANARY
}, prefs = R.string.key_chrome_ip_info, loadAtActivityCreate = ClassNames.ACTIVITY)
public class XposedChromeIPInfo extends XposedChromeBase {

    private AlertDialog mProgressingDialog;

    @Override
    protected void handleLoadPackage() throws Throwable {
        super.handleLoadPackage();
        newMenu(ContextUtils.getLString(R.string.chrome_ip_info), 1007, new NewMenuCallback() {
            @Override
            public void onOptionsItemSelected(final Activity activity, MenuItem item, final String url, boolean isCustomTab) {
                mProgressingDialog = ViewUtils.showProgressingDialog(activity, true,
                    new li.lingfeng.ltweaks.utils.Callback.C0() {
                        @Override
                        public void onResult() {
                            Logger.i("Cancelled by user.");
                            mProgressingDialog = null;
                        }
                    });

                ThreadUtils.runInBackground(new li.lingfeng.ltweaks.utils.Callback.C0() {
                    @Override
                    public void onResult() {
                        try {
                            tryGetIpInfo(activity, url);
                        } catch (Throwable e) {
                            error(activity, e);
                        }
                    }
                });
            }
        });
    }

    private void tryGetIpInfo(final Activity activity, String url) throws Throwable {
        Logger.i("Try to get ip info " + url);
        final String host = new URL(url).getHost();
        InetAddress address = InetAddress.getByName(host);
        final String ip = address.getHostAddress();
        String lang = Locale.getDefault().getLanguage().equals(new Locale("zh")) ? "zh-CN" : "en-US";

        Request request = new Request.Builder()
                .url("https://clientapi.ipip.net/browser/chrome?ip=" + ip + "&domain=" + host + "&l=" + lang)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36")
                .build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                error(activity, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200) {
                    final String str = response.body().string();
                    Logger.i("Got ip info from ipip.net, " + str);

                    try {
                        String html = parseIpInfo(host, ip, str);
                        success(activity, html);
                    } catch (Throwable e) {
                        error(activity, e);
                    }
                } else {
                    error(activity, new RuntimeException("response code " + response.code()));
                }
            }
        });
    }

    private String parseIpInfo(String host, String ip, String str) throws Throwable {
        JSONObject jAll = (JSONObject) JSON.parse(str);
        int ret = jAll.getInteger("ret");
        if (ret != 0) {
            throw new RuntimeException("parseIpInfo error, ret is not 0.");
        }

        LinkedHashMap<Integer, String> data = new LinkedHashMap<>(5);
        data.put(R.string.chrome_ip_domain, host);
        data.put(R.string.chrome_ip_ip, ip);

        JSONObject jData = (JSONObject) jAll.get("data");
        data.put(R.string.chrome_ip_geo, jData.getString("country") + " " + jData.getString("province") + " " + jData.getString("city"));
        data.put(R.string.chrome_ip_isp, jData.getString("isp"));
        JSONArray jAsn = jData.getJSONArray("asn");
        String asn = "";
        for (int i = 0; i < jAsn.size(); ++i) {
            if (i != 0) {
                asn += "<br>&nbsp;&nbsp;";
            }
            asn += jAsn.getString(i);
        }
        data.put(R.string.chrome_ip_asn, asn);

        final StringBuilder stringBuilder = new StringBuilder();
        for (LinkedHashMap.Entry<Integer, String> kv : data.entrySet()) {
            stringBuilder.append("<b>");
            stringBuilder.append(ContextUtils.getLString(kv.getKey()));
            stringBuilder.append(":</b><br>&nbsp;&nbsp;");
            stringBuilder.append(kv.getValue());
            stringBuilder.append("<br><br>");
        }
        return stringBuilder.toString();
    }

    private void success(final Activity activity, final String html) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgressingDialog == null) {
                    return;
                }
                mProgressingDialog.dismiss();
                mProgressingDialog = null;
                ViewUtils.showDialog(activity, Html.fromHtml(html));
            }
        });
    }

    private void error(final Activity activity, final Throwable e) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Logger.e("Get IP info error, " + e);
                Logger.stackTrace(e);
                if (mProgressingDialog != null) {
                    mProgressingDialog.dismiss();
                    mProgressingDialog = null;
                }
                Toast.makeText(activity, R.string.error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
