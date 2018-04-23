package li.lingfeng.ltweaks.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Pair;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

/**
 * Created by smallville on 2017/3/29.
 */

public class Utils {

    public static String[] splitFirst(String str, char ch) {
        int pos = str.indexOf(ch);
        return new String[] { str.substring(0, pos), str.substring(pos + 1) };
    }

    public static String[] splitMax(String str, char ch, int max) {
        List<String> list = new ArrayList<>(max);
        String left = str;
        while (list.size() + 1 < max && !left.isEmpty()) {
            int pos = left.indexOf(ch);
            if (pos >= 0) {
                list.add(left.substring(0, pos));
                left = left.substring(pos + 1);
            } else {
                break;
            }
        }
        if (!left.isEmpty()) {
            list.add(left);
        }
        String[] array = new String[list.size()];
        return list.toArray(array);
    }

    public static String[] splitReach(String str, char ch, int reach) {
        String[] strs = StringUtils.split(str, ch);
        if (strs.length >= reach) {
            return strs;
        }
        String[] newStrs = Arrays.copyOf(strs, reach);
        Arrays.fill(newStrs, strs.length, reach, "");
        return newStrs;
    }

    public static String[] splitByLastChar(String str, char ch) {
        int pos = str.lastIndexOf(ch);
        return new String[] { str.substring(0, pos), str.substring(pos + 1) };
    }

    public static boolean pairContains(Pair[] pairs, Object o, boolean isFirst) {
        for (Pair pair : pairs) {
            if (isFirst ? pair.first.equals(o) : pair.second.equals(o)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isUrl(String str) {
        return Patterns.WEB_URL.matcher(str).matches()
                && (str.toLowerCase().startsWith("http://")
                || str.toLowerCase().startsWith("https://"));
    }

    public static MenuItem findMenuItemByTitle(Menu menu, final String title) {
        return findMenuItemBy(menu, new FindMenuItemCallback() {
            @Override
            public boolean onMenuItem(MenuItem item) {
                return title.equals(item.getTitle());
            }
        });
    }

    public static MenuItem findMenuItemById(Menu menu, final int id) {
        return findMenuItemBy(menu, new FindMenuItemCallback() {
            @Override
            public boolean onMenuItem(MenuItem item) {
                return id == item.getItemId();
            }
        });
    }

    private static MenuItem findMenuItemBy(Menu menu, FindMenuItemCallback callback) {
        for (int i = 0; i < menu.size(); ++i) {
            MenuItem item = menu.getItem(i);
            if (callback.onMenuItem(item)) {
                return item;
            }
        }
        return null;
    }

    private interface FindMenuItemCallback {
        boolean onMenuItem(MenuItem item);
    }

    public static class ObfuscatedClassGenerator implements Iterator<String> {

        private String mPrefix;
        private int mMaxIndex;
        private int mCurrentIndex = -1;

        public ObfuscatedClassGenerator(String prefix, int maxDepth /* start from 1 */) {
            if (maxDepth < 1) {
                throw new RuntimeException("maxDepth should > 0");
            }

            mPrefix = prefix;
            // https://zh.wikipedia.org/zh-hans/%E7%AD%89%E6%AF%94%E6%95%B0%E5%88%97
            // Sn = ((a1 * q ^ n) - a1) / (q - 1)
            mMaxIndex = ((int) Math.pow(26, (maxDepth + 1)) - 26) / 25;
        }

        @Override
        public boolean hasNext() {
            return mCurrentIndex + 1 < mMaxIndex;
        }

        @Override
        public String next() {
            ++mCurrentIndex;
            return generateObfuscatedClass(mPrefix, mCurrentIndex);
        }

        private String generateObfuscatedClass(String prefix, int index) {
            return prefix + generateObfuscatedClass(index);
        }

        private String generateObfuscatedClass(int index) {
            String str = "";
            char a = 'a';
            int loop = index / 26;
            if (loop > 0) {
                str += generateObfuscatedClass(loop - 1);
            }
            a = (char) (a + index % 26);
            str += a;
            return str;
        }
    }

    public static void saveObfuscatedClasses(Object object, Context context, String prefName, int _ver) throws Throwable {
        Logger.v("saveObfuscatedClasses " + prefName);
        int versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        SharedPreferences prefs = context.getSharedPreferences(prefName, 0);
        SharedPreferences.Editor editor = prefs.edit().clear()
                .putInt("versionCode", versionCode)
                .putInt("_ver", _ver);

        if (object != null) {
            Field[] fields = object.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                if (field.getType() == Class.class) {
                    Class c = (Class) field.get(object);
                    editor.putString(field.getName(), c.getName());
                } else if (field.getType() == Field.class) {
                    Field f = (Field) field.get(object);
                    editor.putString(field.getName(), f.getDeclaringClass().getName() + " " + f.getName());
                } else if (field.getType() == Method.class) {
                    Method m = (Method) field.get(object);
                    StringBuilder methodStrings = new StringBuilder();
                    Class[] paramTypes = m.getParameterTypes();
                    for (int i = 0; i < paramTypes.length; ++i) {
                        if (i > 0) {
                            methodStrings.append(' ');
                        }
                        methodStrings.append(paramTypes[i].getName());
                    }
                    editor.putString(field.getName(), m.getDeclaringClass().getName() + " " + m.getName()
                            + (methodStrings.length() > 0 ? " " + methodStrings : ""));
                }
            }
        } else {
            editor.putBoolean("_no_match", true);
        }
        editor.apply();
    }

    public static boolean loadObfuscatedClasses(Object object, Context context, String prefName, int _ver, ClassLoader classLoader) throws Throwable {
        Logger.v("loadObfuscatedClasses " + prefName);
        SharedPreferences prefs = context.getSharedPreferences(prefName, 0);
        if (prefs.getInt("_ver", 0) != _ver) {
            throw new Exception("_ver not match.");
        }

        int versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        int existVersionCode = prefs.getInt("versionCode", 0);
        if (versionCode != existVersionCode) {
            throw new Exception("Version code not match.");
        }

        if (prefs.getBoolean("_no_match", false)) {
            return false;
        }

        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.getType() != Class.class && field.getType() != Field.class && field.getType() != Method.class) {
                continue;
            }

            field.setAccessible(true);
            String value = prefs.getString(field.getName(), null);
            if (value == null) {
                throw new Exception("Can't load " + field.getName());
            }

            if (field.getType() == Class.class) {
                Class c = XposedHelpers.findClass(value, classLoader);
                field.set(object, c);
            } else if (field.getType() == Field.class) {
                String[] strings = prefs.getString(field.getName(), "").split(" ");
                if (strings.length != 2) {
                    throw new Exception("Field format not match, " + value);
                }
                Class c = XposedHelpers.findClass(strings[0], classLoader);
                Field f = c.getDeclaredField(strings[1]);
                f.setAccessible(true);
                field.set(object, f);
            } else if (field.getType() == Method.class) {
                String[] strings = prefs.getString(field.getName(), "").split(" ");
                if (strings.length < 2) {
                    throw new Exception("Method format not match, " + value);
                }
                Class c = XposedHelpers.findClass(strings[0], classLoader);
                Class[] parameterTypes = new Class[strings.length - 2];
                for (int i = 0; i < parameterTypes.length; ++i) {
                    parameterTypes[i] = XposedHelpers.findClass(strings[i + 2], classLoader);
                }
                Method m = XposedHelpers.findMethodExact(c, strings[1], parameterTypes);
                m.setAccessible(true);
                field.set(object, m);
            }
        }
        return true;
    }
}
