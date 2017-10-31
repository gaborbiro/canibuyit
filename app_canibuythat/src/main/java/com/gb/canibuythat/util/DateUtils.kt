package com.gb.canibuythat.util

import android.app.DatePickerDialog
import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

/**
 * Only day. No hour, minute, second or millisecond.
 */
fun Date.clearLowerBits(): Date {
    val c = this.toCalendar()
    c.clearLowerBits()
    return c.time
}

/**
 * Only day. No hour, minute, second or millisecond.
 */
fun Calendar.clearLowerBits() {
    this.set(Calendar.HOUR_OF_DAY, 0)
    this.set(Calendar.MINUTE, 0)
    this.set(Calendar.SECOND, 0)
    this.set(Calendar.MILLISECOND, 0)
}

fun Date.toCalendar(): Calendar {
    val c = Calendar.getInstance()
    c.time = this
    return c
}

/**
 * Only day. No hour, minute, second or millisecond.
 */
fun clearLowerBits(): Calendar {
    val c = Calendar.getInstance()
    c.clearLowerBits()
    return c
}

class DateUtils {

    companion object {
        @JvmStatic val FORMAT_MONTH_DAY_YR = SimpleDateFormat("MMM.dd yy")
        @JvmStatic val FORMAT_ISO = SimpleDateFormat("yyyyMMdd'T'HHmmss")
        @JvmStatic val FORMAT_RFC3339 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        @JvmStatic val FORMAT_DATE_TIME = SimpleDateFormat("yyyy/MMM/dd HH:mm")

        /**
         * Only day. No hour, minute, second or millisecond.
         */
        @JvmStatic fun clearLowerBits(c: Calendar) {
            c.clearLowerBits()
        }

        @JvmStatic fun clearLowerBits(date: Date): Date {
            return date.clearLowerBits()
        }

        /**
         * ... `start` ... `end` ...<br></br>
         * <pre>|   |   |  |  |<br></br>-2 -1   0  1  2
        </pre> *
         */
        @JvmStatic fun compare(date: Calendar, start: Calendar, end: Calendar): Int {
            if (start.after(end)) {
                throw IllegalArgumentException("Start date must come before end date")
            }
            if (date.before(start)) {
                return -2
            } else if (date == start) {
                return -1
            } else if (date.timeInMillis >= start.timeInMillis && date.timeInMillis <= end.timeInMillis) {
                return 0
            } else if (date == end) {
                return 1
            } else {
                return 2
            }
        }

        @JvmStatic fun getDatePickerDialog(context: Context, listener: DatePickerDialog.OnDateSetListener, date: Date?): DatePickerDialog {
            return getDatePickerDialog(context, listener, date?.toCalendar() ?: Calendar.getInstance())
        }

        @JvmStatic fun getDatePickerDialog(context: Context, listener: DatePickerDialog.OnDateSetListener, date: Calendar): DatePickerDialog {
            return DatePickerDialog(context, listener, decompose(date)[0], decompose(date)[1], decompose(date)[2])
        }

        @JvmStatic fun decompose(c: Calendar): IntArray {
            return intArrayOf(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE))
        }

        @JvmStatic fun decompose(date: Date): IntArray {
            val c = date.toCalendar()
            return intArrayOf(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE))
        }

        @JvmStatic fun compose(year: Int, month: Int, dayOfMonth: Int): Calendar {
            val c = Calendar.getInstance()
            c.set(year, month, dayOfMonth)
            c.clearLowerBits()
            return c
        }

        @JvmStatic fun isToday(d: Date): Boolean {
            val now = Calendar.getInstance()
            val c = d.toCalendar()
            return c.get(Calendar.ERA) === now.get(Calendar.ERA) &&
                    c.get(Calendar.YEAR) === now.get(Calendar.YEAR) &&
                    c.get(Calendar.DAY_OF_YEAR) === now.get(Calendar.DAY_OF_YEAR)
        }
    }
}
