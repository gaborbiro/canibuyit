package com.gb.canibuyit.feature.spending.data

import com.gb.canibuyit.feature.spending.model.Spending
import com.gb.canibuyit.feature.spending.model.div
import com.gb.canibuyit.feature.spending.model.overlap
import com.gb.canibuyit.feature.spending.model.plus
import com.gb.canibuyit.feature.spending.model.times
import com.gb.canibuyit.feature.spending.persistence.model.ApiSpending
import java.time.LocalDate

object BalanceCalculator {

    /**
     * Calculate how many times the specified `spending` has been
     * applied up until the specified `end` date (usually today) and sum up
     * its value.
     *
     * @param end if null, today is used instead
     *
     * @return two floating point values, the first is the minimum possible value, the second is the maximum possible value
     */
    fun getEstimatedBalance(spending: Spending, start: LocalDate?, end: LocalDate = LocalDate.now()): Balance {
        if (start != null && end < start) {
            throw IllegalArgumentException("Start date must come before end date!")
        }
        if (!spending.enabled) {
            return Balance(0f, 0f, emptyArray())
        }
        var stop = false
        var target = 0f
        var total = 0f
        var occurrenceCount = 0
        val spendingEvents = mutableListOf<SpendingEvent>()
        var movingStart = spending.fromStartDate
        var movingEnd = spending.fromEndDate
        do {
            if (start == null || movingEnd >= start) {
                val targetIncrement = spending.target?.let { -Math.abs(it).toFloat() } ?: spending.value.toFloat()
                if (end >= movingStart) {
                    val overlap =
                        Pair(
                            Pair(start ?: spending.fromStartDate, end.plusDays(1)),
                            Pair(movingStart, movingEnd.plusDays(1))
                        ).overlap(ApiSpending.Cycle.DAYS)
                    val fraction = overlap / (Pair(movingStart, movingEnd.plusDays(1)) / ApiSpending.Cycle.DAYS)
                    target += fraction * targetIncrement
                    total += fraction * spending.value.toFloat()
                    spendingEvents.add(SpendingEvent(movingStart, movingEnd, total))
                } else {
                    stop = true
                }
            }
            movingStart += spending.cycleMultiplier * spending.cycle
            movingEnd += spending.cycleMultiplier * spending.cycle
            spending.occurrenceCount?.let {
                if (++occurrenceCount >= it) {
                    stop = true
                }
            }
        } while (!stop)
        return Balance(total, target, spendingEvents.toTypedArray())
    }
}
