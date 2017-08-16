package li.lingfeng.ltweaks.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Collection;

/**
 * Created by smallville on 2017/3/29.
 */

public class Utils {

    public static String[] splitByLastChar(String str, char ch) {
        int pos = str.lastIndexOf(ch);
        return new String[] { str.substring(0, pos), str.substring(pos + 1) };
    }

    public static boolean pairContains(Pair[] pairs, Object o, boolean isFirst) {
        for (Pair pair : pairs) {
            if (isFirst ? pair.first.equals(o) : pair.second.equals(o)) {
                return true;
            }
        }
        return false;
    }

    public static MenuItem findMenuItemByTitle(Menu menu, final String title) {
        return findMenuItemBy(menu, new FindMenuItemCallback() {
            @Override
            public boolean onMenuItem(MenuItem item) {
                return title.equals(item.getTitle());
            }
        });
    }

    public static MenuItem findMenuItemById(Menu menu, final int id) {
        return findMenuItemBy(menu, new FindMenuItemCallback() {
            @Override
            public boolean onMenuItem(MenuItem item) {
                return id == item.getItemId();
            }
        });
    }

    private static MenuItem findMenuItemBy(Menu menu, FindMenuItemCallback callback) {
        for (int i = 0; i < menu.size(); ++i) {
            MenuItem item = menu.getItem(i);
            if (callback.onMenuItem(item)) {
                return item;
            }
        }
        return null;
    }

    private interface FindMenuItemCallback {
        boolean onMenuItem(MenuItem item);
    }

    // https://stackoverflow.com/questions/8112715/how-to-crop-bitmap-center-like-imageview
    public static Bitmap createCenterCropBitmapFromFile(String filePath, int newWidth, int newHeight) {
        try {
            Logger.d("createCenterCropBitmapFromFile " + filePath + ", new " + newWidth + "x" + newHeight);
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap source = BitmapFactory.decodeFile(filePath, opt);
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
        } catch (Exception e) {
            Logger.e("Failed to createCenterCropBitmapFromFile, " + e);
        }
        return null;
    }

    public static Bitmap bitmapCopy(Bitmap src, int left, int top, int width, int height) {
        Logger.d("bitmapCopy src " + src.getWidth() + "x" + src.getHeight()
                + ", dst " + left + "x" + top + "|" + width + "x" + height);
        Bitmap dst = Bitmap.createBitmap(src, left, top, width, height);
        return dst;
    }
}
