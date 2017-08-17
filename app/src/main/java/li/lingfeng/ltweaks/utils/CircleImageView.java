package li.lingfeng.ltweaks.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/**
 * Created by lilingfeng on 2017/8/16.
 * Reference: http://www.jianshu.com/p/4f55200cea14
 *            https://github.com/hdodenhof/CircleImageView/blob/master/circleimageview/src/main/java/de/hdodenhof/circleimageview/CircleImageView.java
 */

@SuppressLint("AppCompatCustomView")
public class CircleImageView extends ImageView {

    public CircleImageView(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable drawable = getDrawable();
        if (drawable == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }

        Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        } else if (drawable instanceof ColorDrawable) {
            bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }
        if (bitmap == null) {
            Logger.e("CircleImageView bitmap is null.");
            return;
        }

        if (!(drawable instanceof BitmapDrawable)) {
            Canvas tmpCanvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, tmpCanvas.getWidth(), tmpCanvas.getHeight());
            drawable.draw(tmpCanvas);
        }

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas newCanvas = new Canvas(output);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        newCanvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                Math.min(bitmap.getWidth() / 2, bitmap.getHeight() / 2), paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        newCanvas.drawBitmap(bitmap, rect, rect, paint);

        canvas.drawBitmap(output, 0, 0, null);
    }
}
