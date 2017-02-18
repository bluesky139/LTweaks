package li.lingfeng.ltweaks.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import li.lingfeng.ltweaks.utils.Logger;

/**
 * Created by smallville on 2017/1/4.
 */

public class JDActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!getIntent().getAction().equals(Intent.ACTION_VIEW)) {
            Toast.makeText(this, "Not supported.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String text = getIntent().getDataString();
        Logger.i("JDActivity url " + text);
        Pattern[] patterns = {
                Pattern.compile("https?://item\\.jd\\.com/(\\d+)\\.html"),
                Pattern.compile("https?://re\\.jd\\.com/cps/item/(\\d+)\\.html"),
                Pattern.compile("https?://item\\.m\\.jd\\.com/product/(\\d+)\\.html")
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String itemId = matcher.group(matcher.groupCount());
                Logger.i("Got jd item id " + itemId);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("openapp.jdmobile://virtual?params={\"category\":\"jump\",\"des\":\"productDetail\",\"skuId\":\"" + itemId + "\",\"sourceType\":\"Item\",\"sourceValue\":\"view-ware\"}"));
                startActivity(intent);
                finish();
                return;
            }
        }

        Toast.makeText(this, "Not supported.", Toast.LENGTH_SHORT).show();
        finish();
    }
}
