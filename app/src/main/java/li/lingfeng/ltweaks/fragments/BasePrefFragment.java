package li.lingfeng.ltweaks.fragments;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.annotation.StringRes;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import li.lingfeng.ltweaks.lib.PreferenceChange;
import li.lingfeng.ltweaks.lib.PreferenceClick;
import li.lingfeng.ltweaks.lib.PreferenceLongClick;
import li.lingfeng.ltweaks.prefs.Prefs;
import li.lingfeng.ltweaks.utils.ComponentUtils;
import li.lingfeng.ltweaks.utils.Logger;

/**
 * Created by smallville on 2016/12/24.
 */

public class BasePrefFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener,
        AdapterView.OnItemLongClickListener {

    private Map<String, List<Method>> mPrefChangeListeners = new HashMap<>();
    private Map<String, List<Method>> mPrefClickListeners = new HashMap<>();
    private Map<String, List<Method>> mPrefLongClickListeners = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
        Prefs.makeWorldReadable();
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listenPreferenceChanges();
        listenPreferenceClicks();
        listenPreferenceLongClicks();
    }

    private void listenPreferenceChanges() {
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
                        Preference preference = findPreference(pref);
                        if (method.getParameterTypes().length == 2) {
                            Class secondType = method.getParameterTypes()[1];
                            if (secondType == boolean.class) {
                                method.invoke(this, preference, ((SwitchPreference) preference).isChecked());
                            } else if (secondType == String.class) {
                                String path = Prefs.instance().getString(pref, "");
                                method.invoke(this, preference, path);
                            } else {
                                throw new Exception("Not implemented.");
                            }
                        } else if (method.getParameterTypes().length == 1) {
                            method.invoke(this, preference);
                        } else {
                            throw new Exception("Not implemented.");
                        }
                    } catch (Exception e) {
                        Logger.e("Can't invoke preference change method " + method + " at start, " + e.getMessage());
                        Logger.stackTrace(e);
                    }
                }
            }
        }
    }

    private void listenPreferenceClicks() {
        Method[] methods = getClass().getDeclaredMethods();
        for (Method method : methods) {
            PreferenceClick preferenceClick = method.getAnnotation(PreferenceClick.class);
            if (preferenceClick == null) {
                continue;
            }

            for (int pref : preferenceClick.prefs()) {
                List<Method> clickMethods = mPrefClickListeners.get(getString(pref));
                if (clickMethods == null) {
                    clickMethods = new ArrayList<>();
                    clickMethods.add(method);
                    mPrefClickListeners.put(getString(pref), clickMethods);
                }
                listenPreferenceClick(pref);
            }
        }
    }

    private void listenPreferenceLongClicks() {
        Method[] methods = getClass().getDeclaredMethods();
        for (Method method : methods) {
            PreferenceLongClick preferenceLongClick = method.getAnnotation(PreferenceLongClick.class);
            if (preferenceLongClick == null) {
                continue;
            }

            for (int pref : preferenceLongClick.prefs()) {
                List<Method> clickMethods = mPrefLongClickListeners.get(getString(pref));
                if (clickMethods == null) {
                    clickMethods = new ArrayList<>();
                    clickMethods.add(method);
                    mPrefLongClickListeners.put(getString(pref), clickMethods);
                }
            }
        }

        ListView listView = (ListView) getView().findViewById(android.R.id.list);
        listView.setOnItemLongClickListener(this);
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
                    Logger.stackTrace(e);
                }
            }
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        List<Method> clickMethods = mPrefClickListeners.get(preference.getKey());
        if (clickMethods != null) {
            for (Method method : clickMethods) {
                try {
                    method.setAccessible(true);
                    method.invoke(this, preference);
                } catch (Exception e) {
                    Logger.e("Can't invoke preference click method " + method + ", " + e.getMessage());
                    Logger.stackTrace(e);
                }
            }
        }
        return true;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Preference preference = (Preference) parent.getAdapter().getItem(position);
        List<Method> clickMethods = mPrefLongClickListeners.get(preference.getKey());
        if (clickMethods != null) {
            for (Method method : clickMethods) {
                try {
                    method.setAccessible(true);
                    method.invoke(this, preference);
                } catch (Exception e) {
                    Logger.e("Can't invoke preference long click method " + method + ", " + e.getMessage());
                    Logger.stackTrace(e);
                }
            }
        }
        return true;
    }

    public Preference findPreference(@StringRes int key) {
        return findPreference(getString(key));
    }

    public SwitchPreference findSwitchPreference(@StringRes int key) {
        Preference preference = findPreference(key);
        if (!(preference instanceof SwitchPreference)) {
            throw new RuntimeException("Preference " + getString(key) + " is not SwitchPreference.");
        }
        return (SwitchPreference) preference;
    }

    protected void listenPreferenceChange(@StringRes int key) {
        Preference preference = findPreference(key);
        preference.setOnPreferenceChangeListener(this);
    }

    protected void listenPreferenceClick(@StringRes int key) {
        Preference preference = findPreference(key);
        preference.setOnPreferenceClickListener(this);
    }

    protected void uncheckPreferenceByDisabledComponent(@StringRes int key, Class componentCls) {
        uncheckPreferenceByDisabledComponent(key, componentCls.getName());
    }

    protected void uncheckPreferenceByDisabledComponent(@StringRes int key, String componentCls) {
        if (!ComponentUtils.isComponentEnabled(componentCls) && Prefs.instance().getBoolean(key, false)) {
            findSwitchPreference(key).setChecked(false);
        }
    }
}
