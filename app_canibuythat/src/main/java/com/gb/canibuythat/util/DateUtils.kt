package com.gb.canibuythat.util

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
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

fun Date.toZDT() = this.toCalendar().let {
    ZonedDateTime.of(it[Calendar.YEAR], it[Calendar.MONTH] + 1, it[Calendar.DAY_OF_MONTH],
            it[Calendar.HOUR_OF_DAY], it[Calendar.MINUTE], it[Calendar.SECOND],
            it[Calendar.MILLISECOND], ZoneId.systemDefault())
}!!

/**
 * Only day. No hour, minute, second or millisecond.
 */
fun clearLowerBits(): Calendar {
    val c = Calendar.getInstance()
    c.clearLowerBits()
    return c
}

@SuppressLint("SimpleDateFormat")
class DateUtils {

    companion object {
        private var SUFFIXES = arrayOf("0th", "1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th",
                "10th", "11th", "12th", "13th", "14th", "15th", "16th", "17th", "18th", "19th", "20th",
                "21st", "22nd", "23rd", "24th", "25th", "26th", "27th", "28th", "29th", "30th", "31st")

        @JvmStatic
        val FORMAT_ISO = SimpleDateFormat("yyyyMMdd'T'HHmmss")
        @JvmStatic
        val FORMAT_RFC3339 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        @JvmStatic
        val FORMAT_DATE_TIME = SimpleDateFormat("yyyy/MMM/dd HH:mm")

        /**
         * Only day. No hour, minute, second or millisecond.
         */
        @JvmStatic
        fun clearLowerBits(c: Calendar) {
            c.clearLowerBits()
        }

        @JvmStatic
        fun clearLowerBits(date: Date): Date {
            return date.clearLowerBits()
        }

        /**
         * ... `start` ... `end` ...<br></br>
         * <pre>|   |   |  |  |<br></br>-2 -1   0  1  2
        </pre> *
         */
        @JvmStatic
        fun compare(date: Calendar, start: Calendar, end: Calendar): Int {
            if (start.after(end)) {
                throw IllegalArgumentException("Start date must come before end date")
            }
            return when {
                date.before(start) -> -2
                date == start -> -1
                date.timeInMillis >= start.timeInMillis && date.timeInMillis <= end.timeInMillis -> 0
                date == end -> 1
                else -> 2
            }
        }

        @JvmStatic
        fun getDatePickerDialog(context: Context, listener: DatePickerDialog.OnDateSetListener, date: Date?): DatePickerDialog {
            return getDatePickerDialog(context, listener, date?.toCalendar() ?: Calendar.getInstance())
        }

        @JvmStatic
        fun getDatePickerDialog(context: Context, listener: DatePickerDialog.OnDateSetListener, date: Calendar): DatePickerDialog {
            return DatePickerDialog(context, listener, decompose(date)[0], decompose(date)[1], decompose(date)[2])
        }

        @JvmStatic
        fun decompose(c: Calendar): IntArray {
            return intArrayOf(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE))
        }

        @JvmStatic
        fun decompose(date: Date): IntArray {
            val c = date.toCalendar()
            return intArrayOf(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE))
        }

        @JvmStatic
        fun compose(year: Int, month: Int, dayOfMonth: Int): Calendar {
            val c = Calendar.getInstance()
            c.set(year, month, dayOfMonth)
            c.clearLowerBits()
            return c
        }

        @JvmStatic
        fun isToday(d: Date): Boolean {
            val now = Calendar.getInstance()
            val c = d.toCalendar()
            return c[Calendar.ERA] == now[Calendar.ERA] &&
                    c[Calendar.YEAR] == now[Calendar.YEAR] &&
                    c[Calendar.DAY_OF_YEAR] == now[Calendar.DAY_OF_YEAR]
        }

        @JvmStatic
        fun formatDayMonthYear(date: Date): String {
            val dayNumberSuffix = SUFFIXES[date.toCalendar()[Calendar.DAY_OF_MONTH]]
            return if (date.year + 1900 == Calendar.getInstance()[Calendar.YEAR]) {
                SimpleDateFormat("'the $dayNumberSuffix of' MMM")
            } else {
                SimpleDateFormat("'the $dayNumberSuffix of' MMM, yyyy")
            }.format(date)
        }
    }
}
