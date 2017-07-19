package li.lingfeng.ltweaks.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.ShoppingUtils;

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
        String itemId = ShoppingUtils.findItemIdByStore(text, ShoppingUtils.STORE_JD);
        if (itemId != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("openapp.jdmobile://virtual?params={\"category\":\"jump\",\"des\":\"productDetail\",\"skuId\":\"" + itemId + "\",\"sourceType\":\"Item\",\"sourceValue\":\"view-ware\"}"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            finish();
            return;
        }

        Toast.makeText(this, "Not supported.", Toast.LENGTH_SHORT).show();
        finish();
    }
}
