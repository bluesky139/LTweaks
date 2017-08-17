package li.lingfeng.ltweaks.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ComponentUtils;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.IOUtils;
import li.lingfeng.ltweaks.utils.Logger;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by smallville on 2017/2/2.
 */

public class ImageSearchActivity extends AppCompatActivity {

    private static final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");
    private static final String ACTION_IMAGE_SEARCH = PackageNames.L_TWEAKS + ".ACTION_IMAGE_SEARCH";
    private static final int MAX_WIDTH = 400;

    private static final Map<String, String> sEngines = new HashMap<String, String>() {{
        put("Google", "https://www.google.com/searchbyimage?image_url=%s");
        put("TinEye", "https://tineye.com/search/?pluginver=chrome-1.1.5&url=%s");
        put("IQDB", "https://iqdb.org/?url=%s");
    }};
    private String mEngine;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!(getIntent().getAction().equals(Intent.ACTION_SEND)
                || getIntent().getAction().equals(ACTION_IMAGE_SEARCH))
                || !getIntent().getType().startsWith("image/")) {
            Toast.makeText(this, R.string.not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_image_search);
        Uri uri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        if (uri == null) {
            Toast.makeText(this, R.string.not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!ComponentUtils.isAlias(this)) {
            Logger.i("Choose image engine.");
            grantUriPermission(getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent intent = new Intent(ACTION_IMAGE_SEARCH);
            intent.setType(getIntent().getType());
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Choose image engine..."));
            finish();
        } else {
            mEngine = ComponentUtils.getAlias(this);
            Logger.i("Use image engine " + mEngine);
            if (!sEngines.containsKey(mEngine)) {
                Toast.makeText(this, R.string.not_supported, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            Logger.i("Uploading image file " + uri.toString());
            new SearchByImage().execute(uri);
        }
    }

    private class SearchByImage extends AsyncTask<Uri, Void, byte[]> {

        @Override
        protected byte[] doInBackground(Uri... params) {
            Uri uri = params[0];
            byte[] bytes = IOUtils.uri2bytes(uri);

            // resize to smaller for faster uploading.
            try {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap.getWidth() > MAX_WIDTH && bitmap.getHeight() > MAX_WIDTH) {
                    float scale = Math.max((float) MAX_WIDTH / bitmap.getWidth(), (float) MAX_WIDTH / bitmap.getHeight());
                    int width = (int) (bitmap.getWidth() * scale);
                    int height = (int) (bitmap.getHeight() * scale);
                    Logger.d("Resize image " + bitmap.getWidth() + "x" + bitmap.getHeight() + " -> " + width + "x" + height);
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);

                    Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.PNG;
                    String mimeType = ImageSearchActivity.this.getContentResolver().getType(uri);
                    if ("image/jpeg".equals(mimeType)) {
                        compressFormat = Bitmap.CompressFormat.JPEG;
                    } else if ("image/webp".equals(mimeType)) {
                        compressFormat = Bitmap.CompressFormat.WEBP;
                    }
                    byte[] scaledBytes = IOUtils.bitmap2bytes(scaledBitmap, compressFormat);
                    if (scaledBytes != null) {
                        bytes = scaledBytes;
                        Logger.d("Resized image " + compressFormat);
                    }
                }
            } catch (Throwable e) {
                Logger.w("Can't resize image, " + e);
                Logger.stackTrace(e);
            }
            return bytes;
        }

        @Override
        protected void onPostExecute(byte[] bytes) {
            uploadBytes(bytes);
        }
    }

    private void uploadBytes(byte[] bytes) {
        if (bytes == null) {
            Toast.makeText(this, R.string.cant_read_file, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Request request = new Request.Builder()
                .url("http://104.224.152.49:8000/tmp_image/")
                .post(RequestBody.create(MEDIA_TYPE_PNG, bytes))
                .build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                failedUpload("Request failed, " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200) {
                    successfulUpload(response.body().string());
                } else {
                    failedUpload("Request failed, response code " + response.code() + ", " + response.body().string());
                }
            }
        });
    }

    private void failedUpload(String message) {
        Logger.e(message);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ImageSearchActivity.this, R.string.upload_failed, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void successfulUpload(final String location) {
        Logger.i("Image url " + location);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    String finalUrl = String.format(sEngines.get(mEngine), URLEncoder.encode(location, "utf-8"));
                    ContextUtils.startBrowser(ImageSearchActivity.this, finalUrl);
                } catch (Exception e) {
                    Logger.e("Error to search image, " + e.getMessage());
                    Toast.makeText(ImageSearchActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                }
                finish();
            }
        });
    }
}
