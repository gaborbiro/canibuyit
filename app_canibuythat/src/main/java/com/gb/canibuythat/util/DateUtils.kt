package com.gb.canibuythat.util

import android.app.DatePickerDialog
import android.content.Context
import android.widget.DatePicker
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DateUtils {

    companion object {
        private var SUFFIXES = arrayOf("0th", "1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th",
                "10th", "11th", "12th", "13th", "14th", "15th", "16th", "17th", "18th", "19th", "20th",
                "21st", "22nd", "23rd", "24th", "25th", "26th", "27th", "28th", "29th", "30th", "31st")

        @JvmStatic
        val FORMAT_ISO = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
        @JvmStatic
        val FORMAT_RFC3339 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

        @JvmStatic
        fun getDatePickerDialog(context: Context, listener: (DatePicker, Int, Int, Int) -> Unit, date: LocalDate): DatePickerDialog {
            return DatePickerDialog(context, listener, date.year, date.monthValue, date.dayOfMonth)
        }

        @JvmStatic
        fun formatDayMonthYearWithPrefix(date: LocalDate): String {
            val dayNumberSuffix = SUFFIXES[date.dayOfMonth]
            return if (date.year == LocalDate.now().year) {
                DateTimeFormatter.ofPattern("'the $dayNumberSuffix of' MMM")
            } else {
                DateTimeFormatter.ofPattern("'the $dayNumberSuffix of' MMM, yyyy")
            }.format(date)
        }

        @JvmStatic
        fun formatDayMonthYear(date: LocalDate): String {
            return if (date.year == LocalDate.now().year) {
                DateTimeFormatter.ofPattern("dd.MMM")
            } else {
                DateTimeFormatter.ofPattern("dd.MMM, '`'yy")
            }.format(date)
        }

        @JvmStatic
        fun formatDayMonth(date: LocalDate): String {
            return DateTimeFormatter.ofPattern("dd.MMM").format(date)
        }
    }
}

fun LocalDate.isToday() = this == LocalDate.now()