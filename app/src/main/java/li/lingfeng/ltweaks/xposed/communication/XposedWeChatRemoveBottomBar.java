package li.lingfeng.ltweaks.xposed.communication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.ClassNames;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.ContextUtils;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.SimpleDrawer;
import li.lingfeng.ltweaks.utils.ViewUtils;
import li.lingfeng.ltweaks.utils.XposedUtils;
import li.lingfeng.ltweaks.xposed.XposedBase;

/**
 * Created by lilingfeng on 2017/8/24.
 */
@XposedLoad(packages = PackageNames.WE_CHAT, prefs = R.string.key_wechat_remove_bottom_bar)
public class XposedWeChatRemoveBottomBar extends XposedBase {

    private static final String VIEW_PAGER = "com.tencent.mm.ui.base.CustomViewPager";
    private static final String PERSIONAL_INFO = "com.tencent.mm.plugin.setting.ui.setting.SettingsPersonalInfoUI";
    private SimpleDrawer mDrawerLayout;
    private WeakReference mUserInfoDbRef;
    private boolean mGotContentView = true;
    private Method mMethodAvatar;

    @Override
    protected void handleLoadPackage() throws Throwable {
        findAndHookActivity(ClassNames.WE_CHAT_LAUNCHER_UI, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mGotContentView = false;

                List<Method> methods = getPossibleMethodsOfAvatar();
                if (methods == null) {
                    throw new Exception("Can't get methods of avatar.");
                }
                Logger.d("Possible " + methods.size() + " methods of avatar.");

                final List<Unhook> unhooks = new ArrayList<>(methods.size());
                for (final Method method : methods) {
                    Unhook unhook = XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Logger.i("Got avatar method, " + method);
                            mMethodAvatar = method;
                            XposedUtils.unhookAll(unhooks);
                        }
                    });
                    unhooks.add(unhook);
                }
            }
        });

        findAndHookActivity(ClassNames.WE_CHAT_LAUNCHER_UI, "setContentView", View.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                if (mGotContentView) {
                    return;
                }

                // Main view is framelayout.
                final View view = (View) param.args[0];
                if (view.getClass().getName().startsWith(PackageNames.WE_CHAT)
                        || !(view instanceof ViewGroup)) {
                    Logger.d("Skip content view " + view);
                    return;
                }

                // Main view contains view pager.
                Class clsViewPager = findClass(VIEW_PAGER);
                View viewPager = ViewUtils.findViewByType((ViewGroup) view, clsViewPager);
                if (viewPager == null) {
                    Logger.d("Skip content view " + view + ", view pager is not in it.");
                    return;
                }

                Logger.d("Got content view " + view);
                mGotContentView = true;
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        Activity activity = (Activity) param.thisObject;
                        listenOnLayoutChange(activity, view);
                    }
                });
            }
        });

        findAndHookActivity(ClassNames.WE_CHAT_LAUNCHER_UI, "onDestroy", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mDrawerLayout = null;
                mMethodAvatar = null;
                mGotContentView = true;
            }
        });

        findAndHookActivity(ClassNames.WE_CHAT_LAUNCHER_UI, "dispatchKeyEvent", KeyEvent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                KeyEvent keyEvent = (KeyEvent) param.args[0];
                if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(Gravity.LEFT)
                        && keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                        Logger.i("Back is pressed for closing drawer.");
                        mDrawerLayout.closeDrawers();
                    }
                    param.setResult(true);
                }
            }
        });

        // com.tencent.mm.storage.t in v6.5.4
        Class clsUserInfoDb = getUserInfoDbCls("com.tencent.mm.storage.");
        if (clsUserInfoDb == null) {
            clsUserInfoDb = getUserInfoDbCls("com.tencent.mm.storage.a");
        }
        if (clsUserInfoDb == null) {
            throw new Exception("Can't get clsUserInfoDb.");
        }
        Logger.i("Got clsUserInfoDb " + clsUserInfoDb);

        hookAllConstructors(clsUserInfoDb, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.i("Got userInfoDb " + param.thisObject);
                mUserInfoDbRef = new WeakReference(param.thisObject);
            }
        });
    }

    private void listenOnLayoutChange(final Activity activity, final View view) {
        view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                try {
                    boolean isOk = doModification(activity);
                    if (isOk) {
                        view.removeOnLayoutChangeListener(this);
                    } else {
                        Logger.d("Layout is not ready.");
                    }
                } catch (Throwable e) {
                    Logger.e("startHook error, " + e);
                    Logger.stackTrace(e);
                    view.removeOnLayoutChangeListener(this);
                }
            }
        });
    }

    private boolean doModification(Activity activity) throws Throwable {
        final ViewGroup rootView = (ViewGroup) activity.findViewById(android.R.id.content);
        List<RelativeLayout> tabs = ViewUtils.findAllViewByTypeInSameHierarchy(rootView, RelativeLayout.class, 4);
        if (tabs.size() > 0) {
            Logger.i("Got " + tabs.size() + " tabs.");
            boolean isOk = handleWithTabs(activity, rootView, tabs);
            if (!isOk) {
                return false;
            }
        } else {
            throw new Exception("No tabs.");
        }

        String nickName = (String) XposedHelpers.callMethod(mUserInfoDbRef.get(), "get", 4, null);
        String userName = (String) XposedHelpers.callMethod(mUserInfoDbRef.get(), "get", 42, null);
        String originalUserName = (String) XposedHelpers.callMethod(mUserInfoDbRef.get(), "get", 2, null);
        Logger.d("nickName " + nickName + ", userName " + userName + ", originalUserName " + originalUserName);

        String name = !StringUtils.isEmpty(nickName) ? nickName : userName;
        name = !StringUtils.isEmpty(name) ? name : originalUserName;
        mDrawerLayout.getHeaderText().setText(name);
        mMethodAvatar.invoke(null, mDrawerLayout.getHeaderImage(), originalUserName);
        return true;
    }

    private boolean handleWithTabs(final Activity activity, ViewGroup rootView, List<RelativeLayout> tabs) throws Throwable {
        SimpleDrawer.NavItem[] navItems = new SimpleDrawer.NavItem[tabs.size()];
        for (int i = 0; i < tabs.size(); ++i) {
            RelativeLayout tab = tabs.get(i);
            ImageView imageView = ViewUtils.findViewByType(tab, ImageView.class);
            if (imageView.getWidth() == 0) {
                return false;
            }
            Bitmap bitmap = Bitmap.createBitmap(imageView.getWidth(), imageView.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            imageView.draw(canvas);
            BitmapDrawable drawable = new BitmapDrawable(activity.getResources(), bitmap);
            TextView textView = ViewUtils.findViewByType(tab, TextView.class);
            SimpleDrawer.NavItem navItem = new SimpleDrawer.NavItem(drawable, textView.getText(), tab);
            navItems[i] = navItem;
        }
        SimpleDrawer.NavItem headerItem = new SimpleDrawer.NavItem(ContextUtils.getAppIcon(),
                ContextUtils.getAppName(), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClassName(PackageNames.WE_CHAT, PERSIONAL_INFO);
                activity.startActivity(intent);
                mDrawerLayout.closeDrawers();
            }
        });

        FrameLayout allView = ViewUtils.rootChildsIntoOneLayout(activity);
        mDrawerLayout = new SimpleDrawer(activity, allView, navItems, headerItem);
        mDrawerLayout.updateHeaderBackground(Color.parseColor("#393A3F"));
        rootView.addView(mDrawerLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        Logger.i("Drawer is created.");
        ((ViewGroup) tabs.get(0).getParent()).setVisibility(View.GONE);
        return true;
    }

    private Class getUserInfoDbCls(String strPrefixCls) throws Throwable {
        for (char a = 'a'; a <= 'z'; ++a) {
            Class cls = findClass(strPrefixCls + a);
            if (cls == null) {
                break;
            }
            if (checkClsUserInfoDb(cls)) {
                return cls;
            }
        }
        return null;
    }

    private boolean checkClsUserInfoDb(Class cls) throws Throwable {
        if (!Modifier.isFinal(cls.getModifiers()) || cls.getSuperclass() == Object.class) {
            return false;
        }
        Field[] fields = cls.getDeclaredFields();
        for (Field field : fields) {
            if (field.getType() != String[].class || !Modifier.isFinal(field.getModifiers())
                    || !Modifier.isStatic(field.getModifiers()) || !Modifier.isPublic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            String[] s = (String[]) field.get(null);
            if (s == null || s.length == 0) {
                continue;
            }
            if (s[0].contains("CREATE TABLE IF NOT EXISTS userinfo")) {
                return true;
            }
        }
        return false;
    }

    private List<Method> getPossibleMethodsOfAvatar() throws Throwable {
        for (char a = 'a'; a <= 'z'; ++a) {
            Class cls = findClass("com.tencent.mm.pluginsdk.ui." + a);
            if (cls == null) {
                break;
            }
            List<Method> methodList = getPossibleMethodsOfAvatar(cls);
            if (methodList != null) {
                return methodList;
            }
        }
        return null;
    }

    private List<Method> getPossibleMethodsOfAvatar(Class clsAvatarDrawable) throws Throwable {
        if (!BitmapDrawable.class.isAssignableFrom(clsAvatarDrawable)
                || !Modifier.isFinal(clsAvatarDrawable.getModifiers())) {
            return null;
        }

        // com.tencent.mm.pluginsdk.ui.a$a in v6.5.4
        Class clsInterface = null;
        for (char a = 'a'; a <= 'b'; ++a) {
            Class cls = findClass(clsAvatarDrawable.getName() + "$" + a);
            if (cls.isInterface()) {
                clsInterface = cls;
                break;
            }
        }
        if (clsInterface == null) {
            return null;
        }
        try {
            clsInterface.getDeclaredMethod("doInvalidate");
        } catch (NoSuchMethodException e) {
            Logger.e("Can't get doInvalidate()");
            return null;
        }

        // com.tencent.mm.pluginsdk.ui.a$b in v6.5.4
        Class clsAvatar = null;
        for (char a = 'a'; a <= 'b'; ++a) {
            Class cls = findClass(clsAvatarDrawable.getName() + "$" + a);
            if (!cls.isInterface() && Modifier.isStatic(cls.getModifiers())
                    && Modifier.isPublic(cls.getModifiers())) {
                clsAvatar = cls;
                break;
            }
        }
        if (clsAvatar == null) {
            return null;
        }

        // com.tencent.mm.pluginsdk.ui.a$b.l() in v6.5.4
        List<Method> methodList = new ArrayList<>();
        Method[] methods = clsAvatar.getDeclaredMethods();
        for (Method method : methods) {
            if (Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers())
                    && method.getReturnType() == void.class && method.getParameterTypes().length == 2
                    && method.getParameterTypes()[0] == ImageView.class
                    && method.getParameterTypes()[1] == String.class) {
                methodList.add(method);
            }
        }
        if (methodList.isEmpty()) {
            return null;
        }
        return methodList;
    }
}
