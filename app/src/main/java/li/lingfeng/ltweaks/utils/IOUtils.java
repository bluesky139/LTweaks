package li.lingfeng.ltweaks.utils;

import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import li.lingfeng.ltweaks.MyApplication;

/**
 * Created by smallville on 2017/2/2.
 */

public class IOUtils {

    public static byte[] uri2bytes(Uri uri) {
        byte[] bytes = null;
        InputStream stream = null;
        try {
            stream = MyApplication.instance().getContentResolver().openInputStream(uri);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[15360];
            int read;
            while ((read = stream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, read);
            }
            bytes = outputStream.toByteArray();
        } catch (Exception e) {
            Logger.e("uri2bytes error, " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (stream != null)
                    stream.close();
            } catch (Exception _) {}
        }
        return bytes;
    }
}
