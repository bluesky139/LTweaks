package li.lingfeng.ltweaks.lib;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ResLoad {
    String[] packages(); // Load for all packages if empty.
    int[] prefs();  // Always load if empty.
}