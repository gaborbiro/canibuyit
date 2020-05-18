package com.gb.canibuyit.util

import java.time.LocalDate
import java.time.Year
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object TimeUtils {
    val monthDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM/dd")
    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MMM/dd")
    val dayFormat = DateTimeFormatter.ofPattern("EEEE ")
    val restFormat = DateTimeFormatter.ofPattern(" 'of' MMM, HH:mm:ss")
}

fun LocalDate.toMonthDay(): String = if (this.year < Year.now().value) format(TimeUtils.dateFormatter) else
    format(TimeUtils.monthDayFormatter)

fun ClosedRange<LocalDate>.iterator(step: ChronoUnit): Iterator<LocalDate> {

    return object : Iterator<LocalDate> {
        private var next = start
        private var hasNext = next <= endInclusive

        override fun hasNext(): Boolean {
            return hasNext
        }

        override fun next(): LocalDate {
            val value = next
            next = next.plus(1, step)
            hasNext = next <= endInclusive
            return value
        }
    }
}