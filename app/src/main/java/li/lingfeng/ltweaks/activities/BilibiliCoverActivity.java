package li.lingfeng.ltweaks.activities;

import android.content.Intent;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.activities.base.HttpLoadingActivity;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;

/**
 * Created by smallville on 2018/4/15.
 */

public class BilibiliCoverActivity extends HttpLoadingActivity {

    @Override
    protected int prepare() {
        if (!getIntent().getAction().equals(Intent.ACTION_SEND) || !getIntent().getType().equals("text/plain")) {
            return R.string.not_supported;
        }
        String url = getUrl();
        if (url == null || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            return R.string.not_supported;
        }
        super.prepare();
        return 0;
    }

    @Override
    protected String getUrl() {
        String text = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        Pattern pattern = Pattern.compile("https?://www.(bilibili.com/video/av\\d+)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String url = "https://m." + matcher.group(1) + ".html";
            Logger.v("url " + url);
            return url;
        }
        return null;
    }

    @Override
    protected void onLoadEnd(int code, String body) {
        if (code == 200) {
            Pattern pattern = Pattern.compile("\"litpic\":\"(https?://[^\"]+)\"");
            Matcher matcher = pattern.matcher(body);
            if (matcher.find()) {
                String coverUrl = matcher.group(1);
                ContextUtils.startBrowser(this, coverUrl);
            } else {
                Logger.e("Can't get cover url from response body, " + body);
                Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
