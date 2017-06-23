package li.lingfeng.ltweaks.utils;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.View;

/**
 * Created by smallville on 2017/2/18.
 */

public class ShareUtils {

    public static void shareClipWithSnackbar(final Activity activity, ClipData clipData) {
        final String text = clipData.getItemCount() > 0 ? clipData.getItemAt(0).getText().toString() : "";
        if (text.isEmpty())
            return;
        SimpleSnackbar.make(activity, "你可以分享复制的内容", SimpleSnackbar.LENGTH_LONG)
                .setAction("分享...", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        intent.putExtra(Intent.EXTRA_TEXT, text);
                        activity.startActivity(intent);
                    }
                })
                .show();
    }

    public static void shareText(Context context, String text) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        context.startActivity(Intent.createChooser(shareIntent, "Share with..."));
    }
}
