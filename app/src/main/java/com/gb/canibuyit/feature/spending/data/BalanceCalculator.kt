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
     * Calculate how much money will the user have spent between the start and end dates.
     *
     * Note: when dealing with spending extrapolation, there is an error margin involved. The answer to the question
     * "how much money will I have spent by the end date" comes with a plus-minus because Spending objects only state in which
     * week/month will the amount be spent, not on which day. If the end date fall in the middle of a week/month,
     * the respective amount may or may not be spent by then. This method cheats a bit: it looks at how far in the week/month
     * the specified end date is and adds a proportional fraction of the Spending's value to the total.
     * I tried implementing it with explicit error margin, but it was a mess. This is a good enough approximation.
     * `fromStartDate` and `fromEndDate` are respected.
     *
     * @param end if null, today is used instead
     *
     * @return spending without targets, spending with targets and a list of (projected) transactions
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
        var index = 0
        do {
            if (start == null || movingEnd >= start) {
                // In lack of a target, we estimate that spending.value is what's actually going to be spent
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
            // Can't use += here, because it can be lossy.
            // LocalDate.of(2019, 1, 31).plusMonths(1).plusMonths(1) is not the same as LocalDate.of(2019, 1, 31).plusMonths(2),
            // because the first plusMonths(1) would adjust the resulting day from 31 to 28 (because February 31st makes no sense)
            index++
            movingStart = spending.fromStartDate + (spending.cycleMultiplier * index * spending.cycle)
            movingEnd = spending.fromEndDate + (spending.cycleMultiplier * index * spending.cycle)
            spending.occurrenceCount?.let {
                if (++occurrenceCount >= it) {
                    stop = true
                }
            }
        } while (!stop)
        return Balance(total, target, spendingEvents.toTypedArray())
    }
}
