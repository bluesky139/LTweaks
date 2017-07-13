package li.lingfeng.ltweaks.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;

/**
 * Created by lilingfeng on 2017/7/12.
 */

public class ZhiHuActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClassName(PackageNames.ZHI_HU, ClassNames.ZHI_HU_MAIN_ACTIVITY);
        intent.setData(getIntent().getData());
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
