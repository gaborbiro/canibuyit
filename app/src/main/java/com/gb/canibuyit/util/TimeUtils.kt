package com.gb.canibuyit.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object TimeUtils {
    val monthDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
}

fun ClosedRange<LocalDate>.iterator(step: ChronoUnit): Iterator<LocalDate> {

    return object : Iterator<LocalDate> {
        private var next = this@iterator.start
        private val finalElement = this@iterator.endInclusive
        private var hasNext = !next.isAfter(this@iterator.endInclusive)
        override fun hasNext(): Boolean = hasNext

        override fun next(): LocalDate {
            val value = next
            if (value >= finalElement) {
                hasNext = false
            } else {
                next = next.plus(1, step)
            }
            ChronoUnit.MONTHS
            return value
        }
    }
}