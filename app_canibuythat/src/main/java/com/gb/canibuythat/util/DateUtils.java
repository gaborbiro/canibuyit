package com.gb.canibuythat.util;

import android.app.DatePickerDialog;
import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {

    public static final SimpleDateFormat FORMAT_MONTH_DAY_YR = new SimpleDateFormat("MMM.dd yy");

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

    public static Calendar clearLowerBits(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    public static Calendar clearLowerBits() {
        Calendar c = Calendar.getInstance();
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

    public static DatePickerDialog getDatePickerDialog(Context context, DatePickerDialog.OnDateSetListener listener, Date date) {
        Calendar c = Calendar.getInstance();
        if (date != null) {
            c.setTime(date);
        }
        return getDatePickerDialog(context, listener, c);
    }

    public static DatePickerDialog getDatePickerDialog(Context context, DatePickerDialog.OnDateSetListener listener, Calendar date) {
        return new DatePickerDialog(context, listener, decompose(date)[0], decompose(date)[1], decompose(date)[2]);
    }

    public static int[] decompose(Calendar c) {
        return new int[]{c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE)};
    }

    public static int[] decompose(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return new int[]{c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE)};
    }

    public static Calendar compose(int year, int month, int dayOfMonth) {
        Calendar c = Calendar.getInstance();
        c.set(year, month, dayOfMonth);
        DateUtils.clearLowerBits(c);
        return c;
    }
}
