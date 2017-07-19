package li.lingfeng.ltweaks.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;

/**
 * Created by lilingfeng on 2017/7/11.
 */

public class DoubanMovieActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClassName(PackageNames.DOUBAN_MOVIE, ClassNames.DOUBAN_MOVIE_INTENT_HANDLER_ACTIVITY);
        intent.setData(getIntent().getData());
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        finish();
    }
}
