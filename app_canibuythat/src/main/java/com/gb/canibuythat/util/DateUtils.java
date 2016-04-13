package com.gb.canibuythat.util;


import java.util.Calendar;
import java.util.Date;


/**
 * Created by GABOR on 2015-jan.-31.
 */
public class DateUtils {

	public static Date getDay(int year, int month, int dayOfMonth) {
        Calendar c = clearLowerBits(Calendar.getInstance());
		c.set(Calendar.YEAR, year);
		c.set(Calendar.MONTH, month);
		c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
		return c.getTime();
	}


	/**
	 * Only day. No hour, minute, second, millisecond.
	 */
	public static Calendar clearLowerBits(Calendar c) {
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
        return c;
	}
}