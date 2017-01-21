package li.lingfeng.ltweaks.prefs;

import android.support.annotation.StringRes;
import android.support.annotation.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Created by smallville on 2017/1/21.
 */

public class SharedPreferences {

    private android.content.SharedPreferences mOriginal;

    public SharedPreferences(android.content.SharedPreferences original) {
        mOriginal = original;
    }

    private String getKeyById(int id) {
        return PrefKeys.getById(id);
    }

    public Map<String, ?> getAll() {
        return mOriginal.getAll();
    }

    @Nullable
    public String getString(String key, @Nullable String defValue) {
        return mOriginal.getString(key, defValue);
    }

    @Nullable
    public String getString(@StringRes int key, @Nullable String defValue) {
        return getString(getKeyById(key), defValue);
    }

    @Nullable
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        return mOriginal.getStringSet(key, defValues);
    }

    @Nullable
    public Set<String> getStringSet(@StringRes int key, @Nullable Set<String> defValues) {
        return getStringSet(getKeyById(key), defValues);
    }

    public int getInt(String key, int defValue) {
        return mOriginal.getInt(key, defValue);
    }

    public int getInt(@StringRes int key, int defValue) {
        return getInt(getKeyById(key), defValue);
    }

    public long getLong(String key, long defValue) {
        return mOriginal.getLong(key, defValue);
    }

    public long getLong(@StringRes int key, long defValue) {
        return getLong(getKeyById(key), defValue);
    }

    public float getFloat(String key, float defValue) {
        return mOriginal.getFloat(key, defValue);
    }

    public float getFloat(@StringRes int key, float defValue) {
        return getFloat(getKeyById(key), defValue);
    }

    public boolean getBoolean(String key, boolean defValue) {
        return mOriginal.getBoolean(key, defValue);
    }

    public boolean getBoolean(@StringRes int key, boolean defValue) {
        return getBoolean(getKeyById(key), defValue);
    }

    public boolean contains(String key) {
        return mOriginal.contains(key);
    }

    public boolean contains(@StringRes int key) {
        return contains(getKeyById(key));
    }
}
