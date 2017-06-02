package li.lingfeng.ltweaks.utils;

/**
 * Created by smallville on 2017/6/2.
 */

public class Callback {
    public static interface C1<A> {
        void onResult(A a);
    }

    public static interface C2<A, B> {
        void onResult(A a, B b);
    }
}
