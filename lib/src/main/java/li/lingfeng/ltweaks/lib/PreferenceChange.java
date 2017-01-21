package li.lingfeng.ltweaks.lib;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by smallville on 2017/1/12.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PreferenceChange {
    int[] prefs();
    boolean refreshAtStart() default false;
}
