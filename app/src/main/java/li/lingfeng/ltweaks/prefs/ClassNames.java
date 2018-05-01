package li.lingfeng.ltweaks.prefs;

import android.os.Build;

/**
 * Created by smallville on 2017/2/11.
 */

public class ClassNames {

    public static final String ACTIVITY = "android.app.Activity";
    public static final String FRAGMENT_MANAGER_IMPL = "android.app.FragmentManagerImpl";

    public static final String APP_COMPAT_ACTIVITY = "android.support.v7.app.AppCompatActivity";
    public static final String FRAGMENT_ACTIVITY = "android.support.v4.app.FragmentActivity";
    public static final String TOOLBAR = "android.support.v7.widget.Toolbar";
    public static final String VIEW_PAGER = "android.support.v4.view.ViewPager";
    public static final String REBIND_REPORTING_HOLDER = "android.support.v7.widget.RebindReportingHolder";
    public static final String TAB_LAYOUT_TAB_VIEW = "android.support.design.widget.TabLayout$TabView";
    public static final String CONSTRAINT_LAYOUT = "android.support.constraint.ConstraintLayout";
    public static final String BOTTOM_NAV_VIEW = "android.support.design.widget.BottomNavigationView";
    public static final String DRAWER_LAYOUT = "android.support.v4.widget.DrawerLayout";

    public static final String ACTIVITY_MANAGER_SERVICE = "com.android.server.am.ActivityManagerService";
    public static final String ALARM_MANAGER_SERVICE = "com.android.server.AlarmManagerService";
    public static final String POWER_MANAGER_SERVICE = "com.android.server.power.PowerManagerService";
    public static final String PACKAGE_PARSER = "android.content.pm.PackageParser";
    public static final String PACKAGE_MANAGER_SERVICE = "com.android.server.pm.PackageManagerService";
    public static final String ACTIVITY_INTENT_RESOLVER = "com.android.server.pm.PackageManagerService$ActivityIntentResolver";
    public static final String PHONE_WINDOW = "com.android.internal.policy.PhoneWindow";

    public static final String QS_TILE_HOST = Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1 ?
            "com.android.systemui.statusbar.phone.QSTileHost" : "com.android.systemui.qs.QSTileHost";
    public static final String INTENT_TILE = "com.android.systemui.qs.tiles.IntentTile";
    public static final String RADIO_INFO = "com.android.settings.RadioInfo";
    public static final String DEVELOPMENT_SETTINGS = "com.android.settings.DevelopmentSettings";

    public static final String TEXT_ACTION_MODE_CALLBACK = "android.widget.Editor.TextActionModeCallback";

    // Douban Movie
    public static final String DOUBAN_MOVIE_SEARCH_ACTIVITY = "com.douban.frodo.search.activity.SearchActivity";
    public static final String DOUBAN_MOVIE_INTENT_HANDLER_ACTIVITY = "com.douban.movie.activity.InnerFacadeActivity";

    // Bilibili
    public static final String BILIBILI_SEARCH_ACTIVITY = "tv.danmaku.bili.ui.search.SearchActivity";

    // Zhi Hu
    public static final String ZHI_HU_MAIN_ACTIVITY = "com.zhihu.android.app.ui.activity.MainActivity";

    // QQ
    public static final String QQ_CHAT_ACTIVITY = "com.tencent.mobileqq.activity.ChatActivity";

    // WeChat
    public static final String WE_CHAT_LAUNCHER_UI = "com.tencent.mm.ui.LauncherUI";
}
