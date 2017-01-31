package li.lingfeng.ltweaks;

import android.app.Application;

/**
 * Created by smallville on 2016/12/24.
 */

public class MyApplication extends Application {

    private static Application instance_;
    public static Application instance() {
        return instance_;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance_ = this;
    }

    public static void setInstanceFromXposed(Application application) {
        instance_ = application;
    }
}
