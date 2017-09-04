package li.lingfeng.ltweaks.activities;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;

import java.util.regex.Matcher;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.IOUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.PermissionUtils;
import li.lingfeng.ltweaks.utils.ZXingUtils;

/**
 * Created by smallville on 2017/2/1.
 */

public class QrCodeActivity extends AppCompatActivity {

    private ProgressBar mProgressBar;
    private TextView mQrcodeText;
    private Button mWeChatButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!getIntent().getAction().equals(Intent.ACTION_SEND) || !getIntent().getType().startsWith("image/")) {
            Toast.makeText(this, R.string.not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_qrcode);
        Uri uri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        if (uri == null) {
            Toast.makeText(this, R.string.not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mQrcodeText = (TextView) findViewById(R.id.qrcode_text);
        mQrcodeText.setTextIsSelectable(true);
        mQrcodeText.setMovementMethod(LinkMovementMethod.getInstance());
        mWeChatButton = (Button) findViewById(R.id.qrcode_wechat_scan);
        mWeChatButton.setOnClickListener(new WeChatButton(uri));

        new DecodeTask().execute(uri);
    }

    private class DecodeTask extends AsyncTask<Uri, Void, Result> {

        @Override
        protected Result doInBackground(Uri... params) {
            return ZXingUtils.decodeQrCode(params[0]);
        }

        @Override
        protected void onPostExecute(Result result) {
            mProgressBar.setVisibility(View.GONE);
            mQrcodeText.setVisibility(View.VISIBLE);
            mWeChatButton.setVisibility(View.VISIBLE);
            if (result == null) {
                mQrcodeText.setText(R.string.share_qrcode_cant_decode);
                mQrcodeText.setTextColor(Color.RED);
            } else {
                String text = result.getText();
                Spannable content = new SpannableString(text);

                Matcher matcher = Patterns.WEB_URL.matcher(text);
                while (matcher.find()) {
                    String url = matcher.group();
                    Logger.d("Got url " + url);
                    content.setSpan(new URLSpan(url), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                mQrcodeText.setText(content);
            }
        }
    }

    private class WeChatButton implements View.OnClickListener {

        private Uri mUri;

        public WeChatButton(Uri uri) {
            mUri = uri;
        }

        @Override
        public void onClick(View v) {
            if (!PermissionUtils.tryPermissions(QrCodeActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                return;
            }

            String path = getExternalFilesDir(null) + "/qrcode_wechat_scan_image";
            if (!IOUtils.saveUriToFile(mUri, path)) {
                Toast.makeText(QrCodeActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setClassName(PackageNames.WE_CHAT, ClassNames.WE_CHAT_LAUNCHER_UI);
                intent.putExtra("LauncherUI.From.Scaner.Shortcut", true);
                intent.putExtra("ltweaks_scannable_image", path);
                intent.setFlags(335544320);
                startActivity(intent);
                QrCodeActivity.this.finish();
            } catch (Throwable e) {
                Toast.makeText(QrCodeActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                Logger.e("Start WeChat image scanner error, " + e);
            }
        }
    }
}
