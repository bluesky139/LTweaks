package li.lingfeng.ltweaks.utils;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import org.apache.commons.lang3.StringUtils;

import li.lingfeng.ltweaks.activities.SelectableTextActivity;
import li.lingfeng.ltweaks.prefs.PackageNames;

/**
 * Created by smallville on 2017/2/18.
 */

public class ShareUtils {

    public static void shareClipWithSnackbar(final Activity activity, ClipData clipData) {
        try {
            if (clipData == null) {
                return;
            }
            final CharSequence text = clipData.getItemCount() > 0 ? clipData.getItemAt(0).getText() : null;
            if (StringUtils.isEmpty(text)) {
                return;
            }
            SimpleSnackbar.make(activity, "Got text", SimpleSnackbar.LENGTH_LONG)
                    .setAction("Select", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            selectText(activity, text.toString());
                        }
                    })
                    .setAction("Share...", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            shareText(activity, text.toString());
                        }
                    })
                    .show();
        } catch (Throwable e) {
            Logger.e("shareClipWithSnackbar error, " + e);
            Logger.stackTrace(e);
        }
    }

    public static void shareText(Context context, String text) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        context.startActivity(Intent.createChooser(shareIntent, "Share with..."));
    }

    public static void selectText(Context context, String text) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        shareIntent.setClassName(PackageNames.L_TWEAKS, SelectableTextActivity.class.getName());
        context.startActivity(shareIntent);
    }
}
