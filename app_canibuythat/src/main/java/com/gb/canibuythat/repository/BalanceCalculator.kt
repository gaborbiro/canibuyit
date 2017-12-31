package com.gb.canibuythat.repository

import com.gb.canibuythat.model.Balance
import com.gb.canibuythat.model.Spending
import com.gb.canibuythat.model.SpendingEvent
import com.gb.canibuythat.model.applyTo
import com.gb.canibuythat.util.DateUtils
import com.gb.canibuythat.util.clearLowerBits
import java.util.*

object BalanceCalculator {

    /**
     * Calculate how many times the specified `spending` has been
     * applied up until the specified `end` date (usually today) and sum up
     * it's value.

     * @param end if null, `today` is used instead
     * *
     * @return two floating point values, the first is the minimum possible value, the second is
     * * the maximum possible value
     */
    fun getEstimatedBalance(spending: Spending, start: Date?, end: Date?): Balance {
        if (start != null && end != null && end.before(start)) {
            throw IllegalArgumentException("Start date must come before end date!")
        }
        var `break` = false
        var definitely = 0f
        var maybe = 0f
        var targetDefinitely = 0f
        var targetMaybe = 0f
        var occurrenceCount = 0
        val spendingEvents = mutableListOf<SpendingEvent>()
        var movingStart = spending.fromStartDate
        var movingEnd = spending.fromEndDate
        val start = start?.clearLowerBits()
        val end = end?.clearLowerBits() ?: Date().clearLowerBits()
        var counter = 0
        do {
            if (start == null || movingEnd.time >= start.time) {
                val r = DateUtils.compare(end, movingStart, movingEnd)
                val target = spending.target?.let { if (it > 0) -it.toFloat() else it.toFloat() } ?: spending.value.toFloat()
                if (r >= -1) { // >= start date
                    if (spending.enabled) {
                        maybe += spending.value.toFloat()
                        targetMaybe += target
                    }
                    if (r > 1) { // > end date
                        if (spending.enabled) {
                            definitely += spending.value.toFloat()
                            targetDefinitely += target
                        }
                    }
                    spendingEvents.add(SpendingEvent(movingStart, movingEnd, definitely, maybe))
                } else {
                    `break` = true
                }
            }
            counter++
            movingStart = spending.cycle.applyTo(spending.fromStartDate, spending.cycleMultiplier * counter)
            movingEnd = spending.cycle.applyTo(spending.fromEndDate, spending.cycleMultiplier * counter)
            spending.occurrenceCount?.let {
                if (++occurrenceCount >= it) {
                    `break` = true
                }
            }
        } while (!`break`)
        return Balance(definitely, maybe, targetDefinitely, targetMaybe, spendingEvents.toTypedArray())
    }
}
