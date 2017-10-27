package li.lingfeng.ltweaks.lib;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by smallville on 2016/12/22.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface XposedLoad {
    String[] packages(); // Load for all packages if empty.
    int[] prefs();  // Always load if empty.
    String loadAtActivityCreate() default ""; // Activity name.
    boolean useRemotePreferences() default false;
    boolean loadPrefsInZygote() default false;
}
