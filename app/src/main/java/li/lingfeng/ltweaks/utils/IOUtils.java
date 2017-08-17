package li.lingfeng.ltweaks.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.net.Uri;

import org.apache.commons.io.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
            byte[] buffer = new byte[524288];
            int read;
            while ((read = stream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, read);
            }
            bytes = outputStream.toByteArray();
        } catch (Throwable e) {
            Logger.e("uri2bytes error, " + e);
            Logger.stackTrace(e);
        } finally {
            try {
                if (stream != null)
                    stream.close();
            } catch (Throwable e) {}
        }
        return bytes;
    }

    public static boolean saveUriToFile(Uri uri, String filePath) {
        Logger.v("Save uri " + uri + " to " + filePath);
        byte[] bytes = uri2bytes(uri);
        if (bytes != null) {
            try {
                File file = new File(filePath);
                FileUtils.writeByteArrayToFile(file, bytes);
                return true;
            } catch (Throwable e) {
                Logger.e("saveUriToFile error, " + e);
            }
        }
        return false;
    }

    public static List<String> readLines(String path) {
        try {
            final File file = new File(path);
            if (!file.exists() || !file.canRead()) {
                Logger.w("File doesn't exist or can't be read, " + path);
                return new ArrayList<>();
            }
            return FileUtils.readLines(file, "utf-8");
        } catch (Throwable e) {
            Logger.w("File can't be read, " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static Bitmap createCenterCropBitmapFromUri(Uri uri, int newWidth, int newHeight) {
        Logger.v("createCenterCropBitmapFromUri " + uri + ", new " + newWidth + "x" + newHeight);
        byte[] bytes = uri2bytes(uri);
        if (bytes == null) {
            return null;
        }
        try {
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap source = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opt);
            return createCenterCropBitmap(source, newWidth, newHeight);
        } catch (Throwable e) {
            Logger.e("BitmapFactory.decodeByteArray() error, " + e);
            return null;
        }
    }

    public static Bitmap createCenterCropBitmapFromFile(String filePath, int newWidth, int newHeight) {
        Logger.v("createCenterCropBitmapFromFile " + filePath + ", new " + newWidth + "x" + newHeight);
        try {
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap source = BitmapFactory.decodeFile(filePath, opt);
            return createCenterCropBitmap(source, newWidth, newHeight);
        } catch (Throwable e) {
            Logger.e("BitmapFactory.decodeFile() error, " + e);
            return null;
        }
    }

    // https://stackoverflow.com/questions/8112715/how-to-crop-bitmap-center-like-imageview
    public static Bitmap createCenterCropBitmap(Bitmap source, int newWidth, int newHeight) {
        if (source == null) {
            Logger.e("createCenterCropBitmap source is null.");
            return null;
        }

        try {
            int sourceWidth = source.getWidth();
            int sourceHeight = source.getHeight();

            // Compute the scaling factors to fit the new height and width, respectively.
            // To cover the final image, the final scaling will be the bigger
            // of these two.
            float xScale = (float) newWidth / sourceWidth;
            float yScale = (float) newHeight / sourceHeight;
            float scale = Math.max(xScale, yScale);

            // Now get the size of the source bitmap when scaled
            float scaledWidth = scale * sourceWidth;
            float scaledHeight = scale * sourceHeight;

            // Let's find out the upper left coordinates if the scaled bitmap
            // should be centered in the new size give by the parameters
            float left = (newWidth - scaledWidth) / 2;
            float top = (newHeight - scaledHeight) / 2;

            // The target rectangle for the new, scaled version of the source bitmap will now
            // be
            RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

            // Finally, we create a new bitmap of the specified size and draw our new,
            // scaled bitmap onto it.
            Logger.d("left " + left + ", top " + top + ", newWidth " + newWidth + ", newHeight " + newHeight);
            Bitmap dest = Bitmap.createBitmap(newWidth, newHeight, source.getConfig());
            Canvas canvas = new Canvas(dest);
            canvas.drawBitmap(source, null, targetRect, null);
            source.recycle();
            return dest;
        } catch (Throwable e) {
            Logger.e("Failed to createCenterCropBitmapFromFile, " + e);
        }
        return null;
    }

    public static Bitmap bitmapCopy(Bitmap src, int left, int top, int width, int height) {
        Logger.v("bitmapCopy src " + src.getWidth() + "x" + src.getHeight()
                + ", dst " + left + "x" + top + "|" + width + "x" + height);
        try {
            Bitmap dst = Bitmap.createBitmap(src, left, top, width, height);
            return dst;
        } catch (Throwable e) {
            Logger.e("bitmapCopy error, " + e);
            return null;
        }
    }

    public static byte[] bitmap2bytes(Bitmap bitmap, Bitmap.CompressFormat compressFormat) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] bytes = null;
            if (bitmap.compress(compressFormat, 100, outputStream)) {
                bytes = outputStream.toByteArray();
            }
            return bytes;
        } catch (Throwable e) {
            Logger.e("bitmap2bytes error, " + e);
            return null;
        }
    }
}
