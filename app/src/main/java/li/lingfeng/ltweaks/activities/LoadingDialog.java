package li.lingfeng.ltweaks.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.ProgressBar;

import li.lingfeng.ltweaks.utils.Logger;

/**
 * Created by smallville on 2017/6/2.
 */

public class LoadingDialog {

    private static AlertDialog sDialog;

    public static void show(Activity activity) {
        if (sDialog != null) {
            Logger.w("LoadingDialog is already exist.");
            return;
        }
        sDialog = new AlertDialog.Builder(activity)
                .setView(new ProgressBar(activity))
                .create();
        sDialog.show();
    }

    public static void dismiss() {
        if (sDialog != null) {
            sDialog.dismiss();
            sDialog = null;
        }
    }
}
