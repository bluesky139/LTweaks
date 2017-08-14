package li.lingfeng.ltweaks.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ComponentUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.PackageUtils;

/**
 * Created by lilingfeng on 2017/6/30.
 */

public class ProcessTextActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!getIntent().getAction().equals(Intent.ACTION_PROCESS_TEXT)
                || !getIntent().getType().equals("text/plain")
                || !ComponentUtils.isAlias(this)) {
            Toast.makeText(this, R.string.not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String text = getIntent().getStringExtra(Intent.EXTRA_PROCESS_TEXT);
        if (text == null || text.isEmpty()) {
            Toast.makeText(this, R.string.not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String name = ComponentUtils.getAlias(this);
        Logger.i("ProcessText " + text + " with " + name);
        try {
            Method method = ProcessTextActivity.class.getDeclaredMethod(StringUtils.uncapitalize(name), String.class);
            method.invoke(this, text);
        } catch (Exception e) {
            Logger.e("ProcessTextActivity invoke error, " + e);
            Logger.stackTrace(e);
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private void doubanMovie(String text) {
        Intent intent = new Intent();
        intent.setClassName(PackageNames.DOUBAN_MOVIE, ClassNames.DOUBAN_MOVIE_SEARCH_ACTIVITY);
        intent.putExtra("search_entry", "all");
        intent.putExtra("query", text);
        startActivity(intent);
    }

    private void bilibili(String text) {
        Intent intent = new Intent(Intent.ACTION_SEARCH);
        intent.setClassName(PackageUtils.isPackageInstalled(PackageNames.BILIBILI_IN)
                ? PackageNames.BILIBILI_IN : PackageNames.BILIBILI, ClassNames.BILIBILI_SEARCH_ACTIVITY);
        intent.putExtra("query", text);
        startActivity(intent);
    }
}
