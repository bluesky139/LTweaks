package li.lingfeng.ltweaks.fragments;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.StringRes;
import android.view.MenuItem;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.lib.PreferenceChange;

/**
 * Created by smallville on 2016/12/24.
 */

public class BasePrefFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    private Map<String, List<Method>> mPrefChangeListeners = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listenPreferences();
    }

    private void listenPreferences() {
        Method[] methods = getClass().getDeclaredMethods();
        for (Method method : methods) {
            PreferenceChange preferenceChange = method.getAnnotation(PreferenceChange.class);
            if (preferenceChange == null) {
                continue;
            }

            for (int pref : preferenceChange.prefs()) {
                List<Method> changeMethods = mPrefChangeListeners.get(getString(pref));
                if (changeMethods == null) {
                    changeMethods = new ArrayList<>();
                    changeMethods.add(method);
                    mPrefChangeListeners.put(getString(pref), changeMethods);
                }
                listenPreferenceChange(pref);

                if (preferenceChange.refreshAtStart()) {
                    try {
                        method.setAccessible(true);
                        method.invoke(this, findPreference(pref), null);
                    } catch (Exception e) {
                        Logger.e("Can't invoke preference change method " + method + " at start, " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            getActivity().finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        List<Method> changeMethods = mPrefChangeListeners.get(preference.getKey());
        if (changeMethods != null) {
            for (Method method : changeMethods) {
                try {
                    method.setAccessible(true);
                    method.invoke(this, preference, newValue);
                } catch (Exception e) {
                    Logger.e("Can't invoke preference change method " + method + ", " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    public Preference findPreference(@StringRes int key) {
        return findPreference(getString(key));
    }

    protected void listenPreferenceChange(@StringRes int key) {
        Preference preference = findPreference(key);
        preference.setOnPreferenceChangeListener(this);
    }
}
