package com.gb.canibuythat.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

val SUFFIXES = arrayOf("0th", "1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th",
        "10th", "11th", "12th", "13th", "14th", "15th", "16th", "17th", "18th", "19th", "20th",
        "21st", "22nd", "23rd", "24th", "25th", "26th", "27th", "28th", "29th", "30th", "31st")

val FORMAT_RFC3339: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

fun LocalDate.isToday() = this == LocalDate.now()

fun LocalDate.formatDayMonthYear(): String = if (this.year == LocalDate.now().year) {
    DateTimeFormatter.ofPattern("dd.MMM")
} else {
    DateTimeFormatter.ofPattern("dd.MMM, '`'yy")
}.format(this)

fun LocalDate.formatDayMonthYearWithPrefix(): String {
    val dayNumberSuffix = SUFFIXES[dayOfMonth]
    return if (year == LocalDate.now().year) {
        DateTimeFormatter.ofPattern("'the $dayNumberSuffix of' MMM")
    } else {
        DateTimeFormatter.ofPattern("'the $dayNumberSuffix of' MMM, yyyy")
    }.format(this)
}

fun LocalDate.formatDayMonth(): String = DateTimeFormatter.ofPattern("dd.MMM").format(this)
