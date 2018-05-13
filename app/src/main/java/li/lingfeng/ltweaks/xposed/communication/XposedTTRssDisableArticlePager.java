package li.lingfeng.ltweaks.xposed.communication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by smallville on 2018/5/13.
 */
@XposedLoad(packages = PackageNames.TT_RSS, prefs = R.string.key_ttrss_disable_article_pager)
public class XposedTTRssDisableArticlePager extends XposedBase {

    private static final String ARTICLE_PAGER = "org.fox.ttrss.ArticlePager";
    private static final String ARTICLE_LIST = "org.fox.ttrss.types.ArticleList";
    private static final String DETAIL_ACTIVITY = "org.fox.ttrss.DetailActivity";

    @Override
    protected void handleLoadPackage() throws Throwable {
        hookAllMethods(ARTICLE_PAGER, "initialize", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Logger.v("Change to one article.");
                Object article = param.args[0];
                List articleList = (List) XposedHelpers.newInstance(findClass(ARTICLE_LIST));
                articleList.add(article);
                XposedHelpers.setObjectField(param.thisObject, "m_articles", articleList);
            }
        });

        findAndHookMethod(ARTICLE_PAGER, "onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Bundle savedInstanceState = (Bundle) param.args[2];
                if (savedInstanceState != null) {
                    if (XposedHelpers.getObjectField(param.thisObject, "m_activity").getClass().getName().equals(DETAIL_ACTIVITY)) {
                        Object article = XposedHelpers.getObjectField(param.thisObject, "m_article");
                        List articleList = (List) XposedHelpers.newInstance(findClass(ARTICLE_LIST));
                        articleList.add(article);
                        XposedHelpers.setObjectField(param.thisObject, "m_articles", articleList);
                    }
                    param.args[2] = null;
                }
            }
        });
    }
}
