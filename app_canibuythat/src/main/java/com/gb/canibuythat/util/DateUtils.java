package com.gb.canibuythat.util;

import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.DatePicker;

import com.gb.canibuythat.App;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {

    public static final SimpleDateFormat FORMAT_MONTH_DAY =
            new SimpleDateFormat("MMM.dd");


    public static Date getDayFromDatePicker(DatePicker datePicker) {
        return DateUtils.getDay(datePicker.getYear(), datePicker.getMonth(),
                datePicker.getDayOfMonth());
    }

    public static Date getDay(int year, int month, int dayOfMonth) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        DateUtils.clearLowerBits(c);
        return c.getTime();
    }

    /**
     * Only day. No hour, minute, second or millisecond.
     */
    public static Date clearLowerBits(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public static void clearLowerBits(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    public static int compare(Calendar date, Calendar start, Calendar end) {
        if (start.after(end)) {
            throw new IllegalArgumentException("Start date must come before end date");
        }
        if (date.before(start)) {
            return -2;
        } else if (date.equals(start)) {
            return -1;
        } else if (date.getTimeInMillis() >= start.getTimeInMillis() &&
                date.getTimeInMillis() <= end.getTimeInMillis()) {
            return 0;
        } else if (date.equals(end)) {
            return 1;
        } else {
            return 2;
        }
    }

    public static DatePickerDialog getDatePickerDialog(Context context,
            DatePickerDialog.OnDateSetListener listener, Date date) {
        Calendar c = Calendar.getInstance();
        if (date != null) {
            c.setTime(date);
        }
        return getDatePickerDialog(context, listener, c);
    }

    public static DatePickerDialog getDatePickerDialog(Context context,
            DatePickerDialog.OnDateSetListener listener, Calendar date) {
        return new DatePickerDialog(context, listener,
                date.get(Calendar.YEAR), date.get(Calendar.MONTH),
                date.get(Calendar.DAY_OF_MONTH));
    }
}
