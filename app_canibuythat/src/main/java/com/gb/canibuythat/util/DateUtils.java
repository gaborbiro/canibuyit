package com.gb.canibuythat.util;

import android.widget.DatePicker;

import com.gb.canibuythat.model.BudgetItem;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {

    public static final SimpleDateFormat SHORT_DATE_FORMAT =
            new SimpleDateFormat("MMM. dd");

    public static Date getDayFromDatePicker(DatePicker datePicker) {
        return DateUtils.getDay(datePicker.getYear(), datePicker.getMonth(),
                datePicker.getDayOfMonth());
    }

    public static Date getDay(int year, int month, int dayOfMonth) {
        Calendar c = clearLowerBits(Calendar.getInstance());
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        return c.getTime();
    }

    /**
     * Only day. No hour, minute, second or millisecond.
     */
    public static Calendar clearLowerBits(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
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
}
