package li.lingfeng.ltweaks.xposed.entertainment;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.prefs.PackageNames;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.xposed.XposedBase;

@XposedLoad(packages = PackageNames.COMIC_SCREEN, prefs = R.string.key_comic_screen_better_sort)
public class XposedComicScreenBetterSort extends XposedBase {

    private XC_MethodHook.Unhook mCollectionsSortHook;

    @Override
    protected void handleLoadPackage() throws Throwable {
        mCollectionsSortHook = findAndHookMethod(Collections.class, "sort", List.class, Comparator.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mCollectionsSortHook == null) {
                    return;
                }
                //Logger.d("sort " + param.args[0]);
                //Logger.d("comparator " + param.args[1]);
                Comparator comparator = (Comparator) param.args[1];
                if (comparator != null) {
                    Class compareClass = comparator.getClass().getSuperclass();
                    if (compareClass != Object.class) {
                        Method[] methods = XposedHelpers.findMethodsByExactParameters(compareClass, int.class, String.class, String.class);
                        if (methods.length == 1) {
                            Logger.d("Got compare method " + methods[0]);
                            mCollectionsSortHook = null;
                            hookCompareMethod(methods[0]);
                        }
                    }
                }
            }
        });
    }

    private void hookCompareMethod(Method method) {
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String s1 = (String) param.args[0];
                String s2 = (String) param.args[1];
                int result = new WindowsExplorerStringComparator().compare(s1, s2);
                param.setResult(result);
            }
        });
    }

    // https://stackoverflow.com/questions/3066742/sort-files-by-name-in-java-differs-from-windows-explorer
    // plus backslash handled.
    private static class WindowsExplorerStringComparator //implements Comparator<String>
    {
        private String str1, str2;
        private int pos1, pos2, len1, len2;

        public int compare(String s1, String s2)
        {
            str1 = s1;
            str2 = s2;
            len1 = str1.length();
            len2 = str2.length();
            pos1 = pos2 = 0;

            int result = 0;
            while (result == 0 && pos1 < len1 && pos2 < len2)
            {
                char ch1 = str1.charAt(pos1);
                char ch2 = str2.charAt(pos2);

                if (ch1 == '/')
                {
                    result = ch2 == '/' ? 0 : -1;
                }
                else if (ch2 == '/')
                {
                    result = ch1 == '/' ? 0 : 1;
                }
                else if (Character.isDigit(ch1))
                {
                    result = Character.isDigit(ch2) ? compareNumbers() : -1;
                }
                else if (Character.isLetter(ch1))
                {
                    result = Character.isLetter(ch2) ? compareOther(true) : 1;
                }
                else
                {
                    result = Character.isDigit(ch2) ? 1
                            : Character.isLetter(ch2) ? -1
                            : compareOther(false);
                }

                pos1++;
                pos2++;
            }

            return result == 0 ? len1 - len2 : result;
        }

        private int compareNumbers()
        {
            int end1 = pos1 + 1;
            while (end1 < len1 && Character.isDigit(str1.charAt(end1)))
            {
                end1++;
            }
            int fullLen1 = end1 - pos1;
            while (pos1 < end1 && str1.charAt(pos1) == '0')
            {
                pos1++;
            }

            int end2 = pos2 + 1;
            while (end2 < len2 && Character.isDigit(str2.charAt(end2)))
            {
                end2++;
            }
            int fullLen2 = end2 - pos2;
            while (pos2 < end2 && str2.charAt(pos2) == '0')
            {
                pos2++;
            }

            int delta = (end1 - pos1) - (end2 - pos2);
            if (delta != 0)
            {
                return delta;
            }

            while (pos1 < end1 && pos2 < end2)
            {
                delta = str1.charAt(pos1++) - str2.charAt(pos2++);
                if (delta != 0)
                {
                    return delta;
                }
            }

            pos1--;
            pos2--;

            return fullLen2 - fullLen1;
        }

        private int compareOther(boolean isLetters)
        {
            char ch1 = str1.charAt(pos1);
            char ch2 = str2.charAt(pos2);

            if (ch1 == ch2)
            {
                return 0;
            }

            if (isLetters)
            {
                ch1 = Character.toUpperCase(ch1);
                ch2 = Character.toUpperCase(ch2);
                if (ch1 != ch2)
                {
                    ch1 = Character.toLowerCase(ch1);
                    ch2 = Character.toLowerCase(ch2);
                }
            }

            return ch1 - ch2;
        }
    }
}
