package li.lingfeng.ltweaks.xposed.communication;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.prefs.Prefs;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2017/1/21.
 */
@XposedLoad(packages = PackageNames.WE_CHAT, prefs = R.string.key_wechat_use_incoming_ringtone)
public class XposedWeChatIncomingRingtone extends XposedBase {
    @Override
    public void handleLoadPackage() throws Throwable {
        findAndHookMethod(MediaPlayer.class, "setDataSource", Context.class, Uri.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Context context = (Context) param.args[0];
                Uri uri = (Uri) param.args[1];
                Logger.i("Setting media source, original is " + uri.toString());

                int idPhonering =context.getResources().getIdentifier("phonering", "raw", "com.tencent.mm");
                if (uri.toString().equals("android.resource://com.tencent.mm/" + idPhonering)) {
                    String path = Prefs.instance().getString(R.string.key_wechat_set_incoming_ringtone, "");
                    param.args[1] = Uri.parse(path);
                    Logger.i("Media source is changed to " + path);
                }
            }
        });
    }
}
