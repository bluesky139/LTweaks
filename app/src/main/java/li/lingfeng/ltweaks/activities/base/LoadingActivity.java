package li.lingfeng.ltweaks.activities.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import li.lingfeng.ltweaks.R;

/**
 * Created by smallville on 2018/4/15.
 */

public abstract class LoadingActivity extends AppCompatActivity {

    protected ProgressBar mProgressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int ret = prepare();
        if (ret > 0) {
            Toast.makeText(this, ret, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_loading);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = 200;
        params.height = 200;
        getWindow().setAttributes(params);

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        startLoad();
    }

    /**
     * To check request parameters, prepare something if necessary.
     * @return 0 means ok, or error string res id.
     */
    protected abstract int prepare();

    protected abstract void startLoad();
}
