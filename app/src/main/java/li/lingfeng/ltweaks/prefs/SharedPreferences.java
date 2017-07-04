package li.lingfeng.ltweaks.prefs;

import android.support.annotation.StringRes;
import android.support.annotation.Nullable;
import android.content.SharedPreferences.Editor;

import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XSharedPreferences;

/**
 * Created by smallville on 2017/1/21.
 */

public class SharedPreferences implements android.content.SharedPreferences {

    private android.content.SharedPreferences mOriginal;

    public SharedPreferences(android.content.SharedPreferences original) {
        mOriginal = original;
    }

    public void reloadIfNecessary() {
        try {
            if (mOriginal instanceof XSharedPreferences) {
                XSharedPreferences pref = (XSharedPreferences) mOriginal;
                if (pref.hasFileChanged()) {
                    pref.reload();
                }
            }
        } catch (Throwable e) {}
    }

    private String getKeyById(int id) {
        return PrefKeys.getById(id);
    }

    @Override
    public Map<String, ?> getAll() {
        reloadIfNecessary();
        return mOriginal.getAll();
    }

    @Override
    @Nullable
    public String getString(String key, @Nullable String defValue) {
        reloadIfNecessary();
        return mOriginal.getString(key, defValue);
    }

    @Nullable
    public String getString(@StringRes int key, @Nullable String defValue) {
        return getString(getKeyById(key), defValue);
    }

    @Override
    @Nullable
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        reloadIfNecessary();
        return mOriginal.getStringSet(key, defValues);
    }

    @Nullable
    public Set<String> getStringSet(@StringRes int key, @Nullable Set<String> defValues) {
        return getStringSet(getKeyById(key), defValues);
    }

    @Override
    public int getInt(String key, int defValue) {
        reloadIfNecessary();
        return mOriginal.getInt(key, defValue);
    }

    public int getInt(@StringRes int key, int defValue) {
        return getInt(getKeyById(key), defValue);
    }

    @Override
    public long getLong(String key, long defValue) {
        reloadIfNecessary();
        return mOriginal.getLong(key, defValue);
    }

    public long getLong(@StringRes int key, long defValue) {
        return getLong(getKeyById(key), defValue);
    }

    @Override
    public float getFloat(String key, float defValue) {
        reloadIfNecessary();
        return mOriginal.getFloat(key, defValue);
    }

    public float getFloat(@StringRes int key, float defValue) {
        return getFloat(getKeyById(key), defValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        reloadIfNecessary();
        return mOriginal.getBoolean(key, defValue);
    }

    public boolean getBoolean(@StringRes int key, boolean defValue) {
        return getBoolean(getKeyById(key), defValue);
    }

    @Override
    public boolean contains(String key) {
        reloadIfNecessary();
        return mOriginal.contains(key);
    }

    public boolean contains(@StringRes int key) {
        return contains(getKeyById(key));
    }

    @Override
    public Editor_ edit() {
        return new Editor_(mOriginal.edit());
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        mOriginal.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        mOriginal.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public class Editor_ implements Editor  {

        private Editor mOriginal;

        public Editor_(Editor original) {
            mOriginal = original;
        }

        @Override
        public Editor_ putString(String key, @Nullable String value) {
            mOriginal.putString(key, value);
            return this;
        }

        public Editor_ putString(@StringRes int key, @Nullable String value) {
            putString(getKeyById(key), value);
            return this;
        }

        @Override
        public Editor_ putStringSet(String key, @Nullable Set<String> values) {
            mOriginal.putStringSet(key, values);
            return this;
        }

        public Editor_ putStringSet(@StringRes int key, @Nullable Set<String> values) {
            putStringSet(getKeyById(key), values);
            return this;
        }

        @Override
        public Editor_ putInt(String key, int value) {
            mOriginal.putInt(key, value);
            return this;
        }

        public Editor_ putInt(@StringRes int key, int value) {
            putInt(getKeyById(key), value);
            return this;
        }

        @Override
        public Editor_ putLong(String key, long value) {
            mOriginal.putLong(key, value);
            return this;
        }

        public Editor_ putLong(@StringRes int key, long value) {
            putLong(getKeyById(key), value);
            return this;
        }

        @Override
        public Editor_ putFloat(String key, float value) {
            mOriginal.putFloat(key, value);
            return this;
        }

        public Editor_ putFloat(@StringRes int key, float value) {
            putFloat(getKeyById(key), value);
            return this;
        }

        @Override
        public Editor_ putBoolean(String key, boolean value) {
            mOriginal.putBoolean(key, value);
            return this;
        }

        public Editor_ putBoolean(@StringRes int key, boolean value) {
            putBoolean(getKeyById(key), value);
            return this;
        }

        @Override
        public Editor_ remove(String key) {
            mOriginal.remove(key);
            return this;
        }

        public Editor_ remove(@StringRes int key) {
            remove(getKeyById(key));
            return this;
        }

        @Override
        public Editor_ clear() {
            mOriginal.clear();
            return this;
        }

        @Override
        public boolean commit() {
            return mOriginal.commit();
        }

        @Override
        public void apply() {
            mOriginal.apply();
        }
    }
}
