package com.gb.canibuythat.util;

import android.widget.DatePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {

    public static final SimpleDateFormat DEFAULT_DATE_FORMAT =
            new SimpleDateFormat("MMM.dd.yyyy");


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
}
