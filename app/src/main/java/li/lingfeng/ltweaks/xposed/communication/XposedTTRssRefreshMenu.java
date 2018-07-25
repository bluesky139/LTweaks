package li.lingfeng.ltweaks.xposed.communication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

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
 * Created by smallville on 2018/5/14.
 */
@XposedLoad(packages = PackageNames.TT_RSS, prefs = R.string.key_ttrss_refresh_menu)
public class XposedTTRssRefreshMenu extends XposedBase {

    private static final String MASTER_ACTIVITY = "org.fox.ttrss.MasterActivity";
    private static final String HEADLINES_FRAGMENT = "org.fox.ttrss.HeadlinesFragment";
    private static final int ITEM_ID = 10001;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookMethod(MASTER_ACTIVITY, "initMenu", new XC_MethodHook() {

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Menu menu = (Menu) XposedHelpers.getObjectField(param.thisObject, "m_menu");
                if (menu != null && menu.findItem(ITEM_ID) == null) {
                    Logger.i("Create refresh menu.");
                    int idMenuGroup = ContextUtils.getIdId("menu_group_headlines");
                    MenuItem menuItem = menu.add(idMenuGroup, ITEM_ID, Menu.NONE, ContextUtils.getLString(R.string.ttrss_refresh));
                    menuItem.setIcon(ContextUtils.getDrawable("ic_refresh"));
                    menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

                    menu.findItem(ContextUtils.getIdId("headlines_select")).setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
                    menu.findItem(ContextUtils.getIdId("headlines_toggle_sort_order")).setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
                }
            }
        });

        findAndHookMethod(MASTER_ACTIVITY, "onOptionsItemSelected", MenuItem.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                MenuItem menuItem = (MenuItem) param.args[0];
                if (menuItem.getItemId() == ITEM_ID) {
                    Logger.i("Refresh headlines.");
                    Object fragmentManager = XposedHelpers.callMethod(param.thisObject, "getSupportFragmentManager");
                    Object headlinesFragment = XposedHelpers.callMethod(fragmentManager, "findFragmentByTag", "headlines");
                    XposedHelpers.callMethod(headlinesFragment, "refresh", false);
                    param.setResult(true);
                }
            }
        });

        findAndHookMethod(HEADLINES_FRAGMENT, "onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                ViewGroup viewGroup = (ViewGroup) param.getResult();
                View fab = ViewUtils.findViewByName(viewGroup, "headlines_fab");
                ViewUtils.removeView(fab);
            }
        });
    }
}
