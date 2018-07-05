package li.lingfeng.ltweaks.xposed.entertainment;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.ViewUtils;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/11/27.
 */
@XposedLoad(packages = {
        PackageNames.BILIBILI,
        PackageNames.BILIBILI_IN
}, prefs = R.string.key_bilibili_get_cover)
public class XposedBilibiliCover extends XposedBase {

    private static final String VIDEO_DETAILS_ACTIVITY = "tv.danmaku.bili.ui.video.VideoDetailsActivity";
    private static final String VIDEO_DETAIL = "tv.danmaku.bili.ui.video.api.BiliVideoDetail";

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookActivity(VIDEO_DETAILS_ACTIVITY, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                final Activity activity = (Activity) param.thisObject;
                ViewGroup toolbar = (ViewGroup) ViewUtils.findViewByName(activity, "nav_top_bar");
                ImageView overflowImageView = (ImageView) ViewUtils.findViewByName(toolbar, "overflow");
                LinearLayout.LayoutParams overflowLayoutParams = (LinearLayout.LayoutParams) overflowImageView.getLayoutParams();

                ImageView imageView = new ImageView(activity);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        overflowLayoutParams.width, overflowLayoutParams.height);
                layoutParams.weight = overflowLayoutParams.weight;
                layoutParams.gravity = overflowLayoutParams.gravity;
                imageView.setLayoutParams(layoutParams);
                imageView.setScaleType(overflowImageView.getScaleType());
                imageView.setImageDrawable(ContextUtils.getDrawable("ic_image"));
                imageView.setBackground(overflowImageView.getBackground());

                LinearLayout parent = (LinearLayout) overflowImageView.getParent();
                parent.addView(imageView, parent.indexOfChild(overflowImageView));
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            GetCover(activity);
                        } catch (Throwable e) {
                            Logger.e("Can't get cover, " + e);
                            Logger.stackTrace(e);
                            Toast.makeText(activity, ContextUtils.getLString(R.string.error), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private void GetCover(Activity activity) throws Throwable {
        Field field = XposedHelpers.findFirstFieldByExactType(activity.getClass(), findClass(VIDEO_DETAIL));
        Object videoDetail = field.get(activity);
        String cover = (String) XposedHelpers.getObjectField(videoDetail, "mCover");
        if (StringUtils.isEmpty(cover)) {
            throw new Exception("Empty cover url.");
        }
        ContextUtils.startBrowser(activity, cover);
    }
}
