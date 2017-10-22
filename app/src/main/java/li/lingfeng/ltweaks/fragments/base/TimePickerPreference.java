package li.lingfeng.ltweaks.fragments.base;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import li.lingfeng.ltweaks.R;

/**
 * Created by smallville on 2017/10/21.
 */

public class TimePickerPreference extends DialogPreference {

    private TimePicker mTimePicker;
    private int mHour;
    private int mMinute;

    public TimePickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        String time = null;
        if (restorePersistedValue) {
            time = getPersistedString("00:00");
        } else {
            time = (String) defaultValue;
        }
        mHour = Integer.parseInt(time.split(":")[0]);
        mMinute = Integer.parseInt(time.split(":")[1]);
        setSummary(time);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected View onCreateDialogView() {
        setPositiveButtonText(R.string.app_ok);
        setNegativeButtonText(R.string.app_cancel);
        mTimePicker = new TimePicker(getContext());
        mTimePicker.setIs24HourView(true);
        return mTimePicker;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mTimePicker.setCurrentHour(mHour);
        mTimePicker.setCurrentMinute(mMinute);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            int hour = mTimePicker.getCurrentHour();
            int minute = mTimePicker.getCurrentMinute();
            String time = String.format("%02d:%02d", hour, minute);
            persistString(time);
            setSummary(time);
        }
    }
}
