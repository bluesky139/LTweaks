package li.lingfeng.ltweaks.xposed.communication;

import android.app.Activity;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ScrollView;

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
 * Created by smallville on 2018/5/13.
 */
@XposedLoad(packages = PackageNames.TT_RSS, prefs = R.string.key_ttrss_open_link_in_browser_menu)
public class XposedTTRssArticleOpenInBrowserMenu extends XposedBase {

    private static final String ONLINE_ACTIVITY = "org.fox.ttrss.OnlineActivity";
    private static final int MENU_OPEN_IN_BROWSER_ID = 10000;
    private static final int MENU_SHARE_ID = 10001;
    private static final int MENU_GO_TOP_ID = 10002;
    private static final int MENU_GO_BOTTOM_ID = 10003;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(ONLINE_ACTIVITY, "initMenu", new XC_MethodHook() {

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Menu menu = (Menu) XposedHelpers.getObjectField(param.thisObject, "m_menu");
                if (menu != null && menu.findItem(MENU_OPEN_IN_BROWSER_ID) == null) {
                    Logger.i("Create open in browser menu.");
                    int idMenuGroup = ContextUtils.getIdId("menu_group_article");

                    MenuItem menuItem = menu.add(idMenuGroup, MENU_OPEN_IN_BROWSER_ID, Menu.NONE, ContextUtils.getLString(R.string.ttrss_open_in_browser));
                    menuItem.setIcon(ContextUtils.getDrawable("ic_action_web_site"));
                    menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

                    menu.add(idMenuGroup, MENU_SHARE_ID, Menu.NONE, ContextUtils.getString("share_share_button"));
                    menu.add(idMenuGroup, MENU_GO_TOP_ID, Menu.NONE, ContextUtils.getLString(R.string.ttrss_go_top));
                    menu.add(idMenuGroup, MENU_GO_BOTTOM_ID, Menu.NONE, ContextUtils.getLString(R.string.ttrss_go_bottom));

                    menu.findItem(ContextUtils.getIdId("toggle_published")).setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
                }
            }
        });

        findAndHookMethod(ONLINE_ACTIVITY, "onOptionsItemSelected", MenuItem.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                MenuItem menuItem = (MenuItem) param.args[0];
                switch (menuItem.getItemId()) {
                    case MENU_OPEN_IN_BROWSER_ID: {
                        Logger.i("Open link in browser.");
                        Object article = getArticleFromActivity(activity);
                        String link = (String) XposedHelpers.getObjectField(article, "link");
                        XposedHelpers.callMethod(param.thisObject, "openUri", Uri.parse(link));
                        break;
                    }
                    case MENU_SHARE_ID: {
                        Logger.i("Share.");
                        Object article = getArticleFromActivity(activity);
                        XposedHelpers.callMethod(activity, "shareArticle", article);
                        break;
                    }
                    case MENU_GO_TOP_ID: {
                        Logger.i("Go top.");
                        ScrollView scrollView = getScrollViewFromActivity(activity);
                        scrollView.scrollTo(0, 0);
                        break;
                    }
                    case MENU_GO_BOTTOM_ID: {
                        Logger.i("Go bottom.");
                        ScrollView scrollView = getScrollViewFromActivity(activity);
                        scrollView.scrollTo(0, scrollView.getChildAt(0).getHeight());
                        break;
                    }
                    default:
                        return;
                }
                param.setResult(true);
            }
        });
    }

    private Object getArticleFromActivity(Activity activity) {
        Object fragmentManager = XposedHelpers.callMethod(activity, "getSupportFragmentManager");
        Object articlePager = XposedHelpers.callMethod(fragmentManager, "findFragmentByTag", "article");
        return XposedHelpers.callMethod(articlePager, "getSelectedArticle");
    }

    private WebView getWebViewFromActivity(Activity activity) {
        Object fragmentManager = XposedHelpers.callMethod(activity, "getSupportFragmentManager");
        Object articlePager = XposedHelpers.callMethod(fragmentManager, "findFragmentByTag", "article");
        Object pagerAdapter = XposedHelpers.getObjectField(articlePager, "m_adapter");
        Object fragment = XposedHelpers.callMethod(pagerAdapter, "getCurrentFragment");
        return (WebView) XposedHelpers.getObjectField(fragment, "m_web");
    }

    private ScrollView getScrollViewFromActivity(Activity activity) {
        Object fragmentManager = XposedHelpers.callMethod(activity, "getSupportFragmentManager");
        Object articlePager = XposedHelpers.callMethod(fragmentManager, "findFragmentByTag", "article");
        Object pagerAdapter = XposedHelpers.getObjectField(articlePager, "m_adapter");
        Object fragment = XposedHelpers.callMethod(pagerAdapter, "getCurrentFragment");
        return (ScrollView) XposedHelpers.getObjectField(fragment, "m_contentView");
    }
}
