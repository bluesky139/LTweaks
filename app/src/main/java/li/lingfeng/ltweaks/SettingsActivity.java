package li.lingfeng.ltweaks;


import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;

import li.lingfeng.ltweaks.prefs.Prefs;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.PermissionUtils;
import li.lingfeng.ltweaks.utils.ViewUtils;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Prefs.moveToN();
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_about:
            {
                showAbout();
                return true;
            }
            case R.id.menu_save_log:
            {
                File file = saveLog();
                if (file != null) {
                    ViewUtils.showDialog(this, getString(R.string.app_save_log_ok, file.getAbsolutePath()));
                } else {
                    Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            case R.id.menu_open_log:
            {
                File file = saveLog();
                if (file != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(file), "text/plain");
                    startActivity(intent);
                } else {
                    Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            case R.id.menu_send_mail:
            {
                File file = saveLog();
                if (file != null) {
                    sendLogWithMail(file);
                } else {
                    Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            case R.id.menu_send_to:
            {
                File file = saveLog();
                if (file != null) {
                    sendLogTo(file);
                } else {
                    Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            case R.id.menu_submit_issue:
            {
                ContextUtils.startBrowser(this, "https://github.com/bluesky139/LTweaks/issues");
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private File saveLog() {
        if (!PermissionUtils.tryPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            return null;
        }

        try {
            File srcFile = new File("/data/data/de.robv.android.xposed.installer/log/error.log");
            File dstFile = new File(getExternalFilesDir(null), "error.log");
            FileUtils.copyFile(srcFile, dstFile);
            return dstFile;
        } catch (Exception e) {
            Toast.makeText(this, R.string.app_save_log_error, Toast.LENGTH_SHORT).show();
            Logger.e("Save log error, " + e);
            return null;
        }
    }

    private void sendLogWithMail(File file) {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:"));
            intent.putExtra(Intent.EXTRA_EMAIL, new String[] { "bluesky139+play@gmail.com" });
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            intent.putExtra(Intent.EXTRA_SUBJECT, "[" + getString(R.string.app_name) + "] v" +
                    getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.app_send_mail_description));
            startActivity(Intent.createChooser(intent, getString(R.string.app_send_mail)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.app_send_log_error, Toast.LENGTH_SHORT).show();
            Logger.e("Send log error, " + e);
            Logger.stackTrace(e);
        }
    }

    private void sendLogTo(File file) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        intent.setType("text/plain");
        startActivity(Intent.createChooser(intent, getString(R.string.app_send_to)));
    }

    private void showAbout() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage(R.string.app_about_summary)
                .setPositiveButton(R.string.app_ok, null)
                .show();
        ((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }
}
