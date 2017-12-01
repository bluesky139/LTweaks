package li.lingfeng.ltweaks.fragments.sub.system;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.support.v7.app.AlertDialog;
import android.util.Pair;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import li.lingfeng.ltweaks.R;
import li.lingfeng.ltweaks.activities.ListCheckActivity;
import li.lingfeng.ltweaks.prefs.Prefs;
import li.lingfeng.ltweaks.utils.Logger;
import li.lingfeng.ltweaks.utils.Utils;
import li.lingfeng.ltweaks.utils.ViewUtils;

/**
 * Created by lilingfeng on 2017/11/30.
 */

public class TextActionDataProvider extends ListCheckActivity.DataProvider {

    private static final int[] INTERNAL_STRINGS = new int[] {
            R.string.text_action_cut,
            R.string.text_action_copy,
            R.string.text_action_paste,
            R.string.text_action_share,
            R.string.text_action_select_all,
            R.string.text_action_replace
    };

    private static final int MENU_ID_ADD_CUSTOM = 1;
    private static final int MENU_ID_USAGE      = 2;

    class Action {
        static final int TYPE_INTERNAL = 0;
        static final int TYPE_CLASS    = 1;
        static final int TYPE_CUSTOM   = 2;

        int type;
        Object value;
        CharSequence name;
        boolean block = false;

        CharSequence getName() {
            if (name == null) {
                if (value == null) {
                    throw new AssertionError("Action.getName() value is null.");
                }
                switch (type) {
                    case Action.TYPE_INTERNAL:
                    case Action.TYPE_CUSTOM:
                        name = (String) value;
                        break;
                    case Action.TYPE_CLASS:
                        ActivityInfo info = (ActivityInfo) value;
                        name = info.loadLabel(mActivity.getPackageManager());
                        break;
                    default:
                        throw new RuntimeException("Unhandled action type " + type);
                }
            }
            return name;
        }

        // type:name
        String toUniqueString() {
            String str;
            switch (type) {
                case Action.TYPE_INTERNAL:
                    str = "id:";
                    break;
                case Action.TYPE_CLASS:
                    str = "class:";
                    break;
                case Action.TYPE_CUSTOM:
                    str = "custom:";
                    break;
                default:
                    throw new RuntimeException("Unhandled action type " + type);
            }
            return str + getName();
        }

        // order:block:type:name
        String toSaveString(int order, boolean block) {
            return order + ":" + block + ":" + toUniqueString();
        }
    }

    private List<Action> mActions;

    public TextActionDataProvider(ListCheckActivity activity) {
        super(activity);
        final Set<String> savedSet = Prefs.instance().getStringSet(R.string.key_text_actions_set, new HashSet<String>());
        mActions = new ArrayList<>(savedSet.size());

        final Map<String, Pair<Integer, Boolean>> itemMap = new HashMap<>(savedSet.size());
        for (String savedItem : savedSet) {
            String[] strs = Utils.splitMax(savedItem, ':', 4);
            int order = Integer.parseInt(strs[0]);
            boolean block = Boolean.parseBoolean(strs[1]);
            String type = strs[2];
            String name = strs[3];
            itemMap.put(type + ':' + name, Pair.create(order, block));

            // Custom
            if (type.equals("custom")) {
                Action action = new Action();
                action.type = Action.TYPE_CUSTOM;
                action.value = name;
                action.block = block;
                mActions.add(action);
            }
        }

        // Internal strings from TextView.
        for (int i = 0; i < INTERNAL_STRINGS.length; ++i) {
            Action action = new Action();
            action.type = Action.TYPE_INTERNAL;
            action.value = mActivity.getString(INTERNAL_STRINGS[i]);
            Pair<Integer, Boolean> pair = itemMap.get(action.toUniqueString());
            if (pair != null) {
                action.block = pair.second;
            }
            mActions.add(action);
        }

        // ACTION_PROCESS_TEXT
        Intent intent = new Intent(Intent.ACTION_PROCESS_TEXT);
        intent.setType("text/plain");
        List<ResolveInfo> infos = mActivity.getPackageManager().queryIntentActivities(intent, 0);
        for (ResolveInfo info : infos) {
            Action action = new Action();
            action.type = Action.TYPE_CLASS;
            action.value = info.activityInfo;
            Pair<Integer, Boolean> pair = itemMap.get(action.toUniqueString());
            if (pair != null) {
                action.block = pair.second;
            }
            mActions.add(action);
        }


        // Sort
        Collections.sort(mActions, new Comparator<Action>() {
            @Override
            public int compare(Action a1, Action a2) {
                Pair<Integer, Boolean> pair = itemMap.get(a1.toUniqueString());
                Integer order1 = pair == null ? null : pair.first;
                pair = itemMap.get(a2.toUniqueString());
                Integer order2 = pair == null ? null : pair.first;
                if (order1 == null && order2 == null) {
                    return 0;
                }
                if (order1 == null) {
                    return 1;
                }
                if (order2 == null) {
                    return -1;
                }
                return order1 - order2;
            }
        });
    }

    @Override
    protected String getActivityTitle() {
        return mActivity.getString(R.string.pref_text_actions);
    }

    @Override
    protected String[] getTabTitles() {
        return new String[] { mActivity.getString(R.string.text_actions_all) };
    }

    @Override
    protected int getListItemCount(int tab) {
        return mActions.size();
    }

    @Override
    protected ListItem getListItem(int tab, int position) {
        Action action = mActions.get(position);
        ListItem item = new ListItem();
        item.mData = action;
        item.mTitle = action.getName();
        item.mChecked = action.block;
        switch (action.type) {
            case Action.TYPE_INTERNAL:
                item.mIcon = mActivity.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
                item.mDescription = mActivity.getString(R.string.text_actions_system);
                break;
            case Action.TYPE_CLASS:
                ActivityInfo info = (ActivityInfo) action.value;
                item.mIcon = info.loadIcon(mActivity.getPackageManager());
                item.mDescription = info.packageName;
                break;
            case Action.TYPE_CUSTOM:
                item.mIcon = mActivity.getResources().getDrawable(R.drawable.ic_edit);
                item.mDescription = mActivity.getString(R.string.text_actions_custom);
                break;
            default:
                throw new RuntimeException("Unhandled action type " + action.type);
        }
        return item;
    }

    @Override
    protected boolean reload() {
        return false;
    }

    @Override
    protected boolean allowMove() {
        return true;
    }

    @Override
    protected void onMove(int fromPosition, int toPosition) {
        Collections.swap(mActions, fromPosition, toPosition);
        save();
    }

    @Override
    public void onCheckedChanged(ListItem item, Boolean isChecked) {
        item.getData(Action.class).block = isChecked;
        save();
    }

    @Override
    protected void onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_ID_ADD_CUSTOM, MENU_ID_ADD_CUSTOM, R.string.text_actions_menu_add_custom);
        menu.add(Menu.NONE, MENU_ID_USAGE, MENU_ID_USAGE, R.string.text_actions_menu_usage);
    }

    @Override
    protected void onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ID_ADD_CUSTOM:
                final EditText editText = new EditText(mActivity);
                editText.setHint(R.string.text_actions_custom_title);
                new AlertDialog.Builder(mActivity)
                        .setView(editText)
                        .setPositiveButton(R.string.app_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                addCustom(editText.getText().toString());
                            }
                        })
                        .setNegativeButton(R.string.app_cancel, null)
                        .create()
                        .show();
                break;
            case MENU_ID_USAGE:
                ViewUtils.showDialog(mActivity, R.string.text_actions_usage);
                break;
        }
    }

    private void addCustom(String name) {
        if (StringUtils.isBlank(name)) {
            return;
        }
        Logger.i("Add custom text action " + name);
        Action action = new Action();
        action.type = Action.TYPE_CUSTOM;
        action.value = name;
        mActions.add(action);
        save();
        notifyDataSetChanged();
    }

    private void save() {
        Logger.i("Save text actions.");
        Set<String> toSaveSet = new HashSet<>(mActions.size());
        for (int i = 0; i < mActions.size(); ++i) {
            Action action = mActions.get(i);
            toSaveSet.add(action.toSaveString(i, action.block));
        }
        Prefs.instance().edit().putStringSet(R.string.key_text_actions_set, toSaveSet).commit();
    }
}
