package li.lingfeng.ltweaks.utils;

import android.app.Activity;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.View;

import static li.lingfeng.ltweaks.utils.ContextUtils.dp2px;

/**
 * Created by smallville on 2018/3/17.
 */

public class DrawerStruct extends DrawerLayout {

    public DrawerStruct(Activity activity, View mainView, View navLayout) {
        super(activity);
        addView(mainView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        LayoutParams params = new LayoutParams(dp2px(280), LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.LEFT;
        addView(navLayout, params);
    }
}
