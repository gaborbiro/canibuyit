package com.gb.canibuyit.util

import com.gb.canibuyit.db.model.ApiSpending
import com.gb.canibuyit.model.span
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

val SUFFIXES = arrayOf("0th", "1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th",
        "10th", "11th", "12th", "13th", "14th", "15th", "16th", "17th", "18th", "19th", "20th",
        "21st", "22nd", "23rd", "24th", "25th", "26th", "27th", "28th", "29th", "30th", "31st")

val FORMAT_RFC3339 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
val FORMAT_SERVER_EVENT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
val FORMAT_EVENT_TIME = DateTimeFormatter.ofPattern("'Tomorrow at 'HH:mm")

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

fun LocalDateTime.formatEventTimePrefix(): String {
    val dayNumberSuffix = SUFFIXES[dayOfMonth]
    return if (year == LocalDate.now().year) {
        DateTimeFormatter.ofPattern("'$dayNumberSuffix of' MMM, HH:mm")
    } else {
        DateTimeFormatter.ofPattern("'$dayNumberSuffix of' MMM, yyyy, HH:mm")
    }.format(this)
}

fun parseEventDateTime(dateTime: String) = LocalDateTime.parse(dateTime, FORMAT_SERVER_EVENT_TIME)

fun LocalDateTime.formatEventTime() = this.format(FORMAT_EVENT_TIME)

fun LocalDate.formatDayMonth(): String = DateTimeFormatter.ofPattern("dd.MMM").format(this)

fun min(date1: LocalDate, date2: LocalDate) = if (date1 < date2) date1 else date2
fun max(date1: LocalDate, date2: LocalDate) = if (date1 > date2) date1 else date2

fun Pair<Pair<LocalDate, LocalDate>, Pair<LocalDate, LocalDate>>.overlap(cycle: ApiSpending.Cycle) =
        Pair(max(this.first.first, this.second.first), min(this.first.second, this.second.second)).span(cycle)

fun midnight() = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT)

fun LocalDateTime.millisUntil() = LocalDateTime.now().until(this, ChronoUnit.MILLIS)