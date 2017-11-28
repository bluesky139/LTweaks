package li.lingfeng.ltweaks.xposed.entertainment;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
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
import li.lingfeng.ltweaks.xposed.XposedBase;

import static li.lingfeng.ltweaks.utils.ContextUtils.dp2px;

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
    private Activity mActivity;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookActivity(VIDEO_DETAILS_ACTIVITY, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mActivity = (Activity) param.thisObject;
            }
        });

        findAndHookActivity(VIDEO_DETAILS_ACTIVITY, "onPause", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mActivity = null;
            }
        });

        // Bilibili v5.17.0, bl.gwl.showAtLocation()
        findAndHookMethod(PopupWindow.class, "showAtLocation", View.class, int.class, int.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mActivity == null) {
                    return;
                }
                final Activity activity = mActivity;
                mActivity = null;

                PopupWindow popupWindow = (PopupWindow) param.thisObject;
                LinearLayout contentView = (LinearLayout) popupWindow.getContentView();
                String title = ContextUtils.getLString(R.string.bilibili_get_cover);
                View lastView = contentView.getChildAt(contentView.getChildCount() - 1);
                if (lastView instanceof TextView && title.equals(((TextView) lastView).getText())) {
                    return;
                }

                Logger.i("Create menu item to get cover.");
                TextView textView = new TextView(contentView.getContext());
                textView.setText(title);
                textView.setTextSize(14);
                textView.setTextColor(ContextUtils.getColor("theme_color_text_primary"));
                textView.setGravity(Gravity.CENTER);
                textView.setBackgroundResource(ContextUtils.getThemeValue("selectableItemBackground"));
                contentView.addView(textView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp2px(40)));

                textView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Logger.i("Get Cover clicked.");
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

        /*// Bilibili v5.17.0, set image to imageview from url.
        findAndHookMethod("bl.djj", "a", String.class, ImageView.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.d("djj.a() " + param.args[0]);
            }
        });*/
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
