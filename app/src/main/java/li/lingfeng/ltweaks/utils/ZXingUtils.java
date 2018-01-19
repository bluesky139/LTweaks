package li.lingfeng.ltweaks.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.EnumMap;
import java.util.EnumSet;

import li.lingfeng.ltweaks.MyApplication;

/**
 * Created by smallville on 2017/2/1.
 */

public class ZXingUtils {

    private static WeakReference<MultiFormatReader> multiFormatReaderRef;
    public static Result decodeQrCode(Uri uri) {
        MultiFormatReader multiFormatReader = multiFormatReaderRef != null ? multiFormatReaderRef.get() : null;
        if (multiFormatReader == null) {
            multiFormatReader = new MultiFormatReader();
            multiFormatReader.setHints(new EnumMap<DecodeHintType, Object>(DecodeHintType.class) {{
                put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(BarcodeFormat.QR_CODE));
                put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
                put(DecodeHintType.CHARACTER_SET, "utf-8");
            }});
            multiFormatReaderRef = new WeakReference<>(multiFormatReader);
            Logger.d("New multiFormatReader is created.");
        }

        InputStream stream = null;
        Result result = null;
        try {
            Logger.i("Decoding qrcode " + uri.toString());
            stream = MyApplication.instance().getContentResolver().openInputStream(uri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(stream);

            for (int scale : new int[] { 1, 2, 4 }) {
                Bitmap bitmap;
                if (scale == 1) {
                    bitmap = originalBitmap;
                } else {
                    bitmap = Bitmap.createScaledBitmap(originalBitmap, originalBitmap.getWidth() / scale,
                            originalBitmap.getHeight() / scale, false);
                }

                Logger.v("Try " + bitmap.getWidth() + "x" + bitmap.getHeight());
                int[] bitmapArray = new int[bitmap.getWidth() * bitmap.getHeight()];
                bitmap.getPixels(bitmapArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
                LuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), bitmapArray);
                BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
                try {
                    result = multiFormatReader.decodeWithState(binaryBitmap);
                    break;
                } catch (NotFoundException qrNotFound) {}
            }

            if (result == null) {
                throw new RuntimeException("QrCodeNotFoundException");
            }
        } catch (Throwable e) {
            Logger.e("Can't decode qrcode from uri " + uri.toString() + ", " + e);
            Logger.stackTrace(e);
        } finally {
            try {
                if (stream != null)
                    stream.close();
            } catch (Exception e) {}
        }
        return result;
    }
}
