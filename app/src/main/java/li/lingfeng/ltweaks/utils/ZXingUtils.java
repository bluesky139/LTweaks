package li.lingfeng.ltweaks.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
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
            }});
            multiFormatReaderRef = new WeakReference<>(multiFormatReader);
            Logger.d("New multiFormatReader is created.");
        }

        InputStream stream = null;
        Result result = null;
        try {
            Logger.i("Decoding qrcode " + uri.toString());
            stream = MyApplication.instance().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(stream);
            int[] bitmapArray = new int[bitmap.getWidth() * bitmap.getHeight()];
            bitmap.getPixels(bitmapArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
            LuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), bitmapArray);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
            result = multiFormatReader.decodeWithState(binaryBitmap);
        } catch (Exception e) {
            Logger.e("Can't decode qrcode from uri " + uri.toString() + ", " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (stream != null)
                    stream.close();
            } catch (Exception e) {}
        }
        return result;
    }
}
