package li.lingfeng.ltweaks.xposed.communication;

import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2018/5/13.
 */
@XposedLoad(packages = PackageNames.TT_RSS, prefs = R.string.key_ttrss_open_link_in_browser_menu)
public class XposedTTRssArticleOpenInBrowserMenu extends XposedBase {

    private static final String ONLINE_ACTIVITY = "org.fox.ttrss.OnlineActivity";
    private static final int ITEM_ID = 10000;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(ONLINE_ACTIVITY, "initMenu", new XC_MethodHook() {

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Menu menu = (Menu) XposedHelpers.getObjectField(param.thisObject, "m_menu");
                if (menu != null && menu.findItem(ITEM_ID) == null) {
                    Logger.i("Create open in browser menu.");
                    int idMenuGroup = ContextUtils.getIdId("menu_group_article");
                    MenuItem menuItem = menu.add(idMenuGroup, ITEM_ID, Menu.NONE, ContextUtils.getLString(R.string.ttrss_open_in_browser));
                    menuItem.setIcon(ContextUtils.getDrawable("ic_action_web_site"));
                    menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

                    menu.findItem(ContextUtils.getIdId("toggle_published")).setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
                }
            }
        });

        findAndHookMethod(ONLINE_ACTIVITY, "onOptionsItemSelected", MenuItem.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                MenuItem menuItem = (MenuItem) param.args[0];
                if (menuItem.getItemId() == ITEM_ID) {
                    Logger.i("Open link in browser.");
                    Object fragmentManager = XposedHelpers.callMethod(param.thisObject, "getSupportFragmentManager");
                    Object articlePager = XposedHelpers.callMethod(fragmentManager, "findFragmentByTag", "article");
                    Object article = XposedHelpers.callMethod(articlePager, "getSelectedArticle");
                    String link = (String) XposedHelpers.getObjectField(article, "link");
                    XposedHelpers.callMethod(param.thisObject, "openUri", Uri.parse(link));
                    param.setResult(true);
                }
            }
        });
    }
}
