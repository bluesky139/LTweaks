package li.lingfeng.ltweaks.utils;

import android.os.AsyncTask;

/**
 * Created by lilingfeng on 2017/12/5.
 */

public class ThreadUtils {

    public static void runInBackground(final Callback.C0 callback) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                callback.onResult();
                return null;
            }
        }.execute();
    }
}
