package li.lingfeng.ltweaks.fragments.sub.donate;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.PermissionUtils;

/**
 * Created by lilingfeng on 2018/1/18.
 */

public class WeChatDonate {

    public static void donate(final Activity activity) {
        Logger.i("Donate with WeChat.");
        PermissionUtils.requestPermissions(activity, new PermissionUtils.ResultCallback() {
            @Override
            public void onResult(boolean ok) {
                if (ok) {
                    doDonate(activity);
                }
            }
        }, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private static void doDonate(Activity activity) {
        try {
            // Save QrCode png to sdcard.
            InputStream inputStream = activity.getResources().openRawResource(R.raw.donate_wechat);
            File dir = activity.getExternalFilesDir("donate");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, "ltweaks_donate_wechat.png");
            if (file.exists()) {
                file.delete();
            }
            FileOutputStream outputStream = new FileOutputStream(file);
            IOUtils.copy(inputStream, outputStream);

            ContentResolver contentResolver = activity.getContentResolver();
            ContentValues values = new ContentValues(7);
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.ORIENTATION, 0);
            values.put(MediaStore.Images.Media.TITLE, "ltweaks_donate_wechat");
            values.put(MediaStore.Images.Media.DESCRIPTION, "ltweaks_donate_wechat");
            values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
            values.put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000);

            Uri uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                Logger.d("WeChat donate image is inserted to database.");
                long id = ContentUris.parseId(uri);
                MediaStore.Images.Thumbnails.getThumbnail(contentResolver, id, MediaStore.Images.Thumbnails.MINI_KIND, null);
            } else {
                Logger.d("WeChat donate image should already inserted to database.");
            }

            // Launch WeChat scanner.
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(PackageNames.WE_CHAT, ClassNames.WE_CHAT_LAUNCHER_UI));
            intent.putExtra("LauncherUI.From.Scaner.Shortcut", true);
            intent.putExtra("ltweaks_scannable_image", file.getAbsolutePath());
            intent.setFlags(335544320);
            intent.setAction("android.intent.action.VIEW");
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.wechat_not_installed, Toast.LENGTH_SHORT).show();
        } catch (Throwable e) {
            Toast.makeText(activity, R.string.error, Toast.LENGTH_SHORT).show();
            Logger.e("Can't open wechat, " + e);
            Logger.stackTrace(e);
        }
    }
}
