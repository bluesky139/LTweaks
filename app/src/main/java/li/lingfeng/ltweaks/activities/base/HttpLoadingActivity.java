package li.lingfeng.ltweaks.activities.base;

import java.io.IOException;

import li.lingfeng.ltweaks.utils.Logger;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by smallville on 2018/4/16.
 */

public abstract class HttpLoadingActivity extends LoadingActivity {

    protected OkHttpClient mHttpClient;

    @Override
    protected int prepare() {
        mHttpClient = new OkHttpClient();
        return 0;
    }

    protected abstract String getUrl();

    protected String getUrlReferer() {
        return null;
    }

    @Override
    protected void startLoad() {
        try {
            Logger.i("Start request " + getUrl());
            Request.Builder builder = new Request.Builder()
                    .url(getUrl())
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Mobile Safari/537.36");
            if (getUrlReferer() != null) {
                builder.header("Referer", getUrlReferer());
            }
            Request request = builder.build();

            mHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Logger.e("onFailure " + e);
                    handleResponse(-1, null);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Logger.v("onResponse " + response.code());
                    String body = response.body().string();
                    if (response.code() != 200) {
                        Logger.e(body);
                    }
                    handleResponse(response.code(), body);
                }
            });
        } catch (Exception e) {
            Logger.e("Request exception, " + e.getMessage());
            handleResponse(1, null);
        }
    }

    private void handleResponse(final int code, final String body) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onLoadEnd(code, body);
            }
        });
    }

    protected abstract void onLoadEnd(int code, String body);
}
