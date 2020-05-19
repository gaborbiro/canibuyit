package com.gb.canibuyit.util

import com.gb.canibuyit.feature.spending.model.firstCycleDay
import com.gb.canibuyit.feature.spending.persistence.model.DBSpending
import java.time.LocalDate
import java.time.Year
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
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
        private var next: LocalDate
        private var hasNext: Boolean
        private val field: ChronoField?

        init {
            field = arrayOf(ChronoField.DAY_OF_WEEK, ChronoField.DAY_OF_MONTH, ChronoField.DAY_OF_YEAR).firstOrNull { it.baseUnit == step }
            next = start
            hasNext = next <= endInclusive
        }

        override fun hasNext(): Boolean {
            return hasNext
        }

        override fun next(): LocalDate {
            val value = next
            next = next.plus(1, step).let { x -> field?.let { x.with(field, 1) } ?: x }
            hasNext = next <= endInclusive
            return value
        }
    }
}