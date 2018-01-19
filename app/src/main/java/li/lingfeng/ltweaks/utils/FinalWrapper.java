package li.lingfeng.ltweaks.utils;

/**
 * Created by lilingfeng on 2018/1/19.
 */

public class FinalWrapper<T> {

    private T mObj;

    public FinalWrapper() {
    }

    public FinalWrapper(T t) {
        mObj = t;
    }

    public void set(T t) {
        mObj = t;
    }

    public T get() {
        return mObj;
    }
}
