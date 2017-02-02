package li.lingfeng.ltweaks.activities;

import android.content.Intent;
import android.net.Uri;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;

import java.util.regex.Matcher;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.ZXingUtils;

/**
 * Created by smallville on 2017/2/1.
 */

public class QrCodeActivity extends AppCompatActivity {

    private ProgressBar mProgressBar;
    private TextView mQrcodeText;

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
        new ZXingUtils.DecodeTask(mDecodeCallback).execute(uri);
    }

    private ZXingUtils.DecodeCallback mDecodeCallback = new ZXingUtils.DecodeCallback() {
        @Override
        public void onDecoded(Result result) {
            if (result == null) {
                Toast.makeText(QrCodeActivity.this, R.string.share_qrcode_cant_decode, Toast.LENGTH_SHORT).show();
                QrCodeActivity.this.finish();
            } else {
                mProgressBar.setVisibility(View.GONE);
                mQrcodeText.setVisibility(View.VISIBLE);
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
    };
}
