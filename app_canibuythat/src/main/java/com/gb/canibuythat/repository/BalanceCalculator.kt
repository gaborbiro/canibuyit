package com.gb.canibuythat.repository

import com.gb.canibuythat.model.*
import java.time.LocalDate

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
    fun getEstimatedBalance(spending: Spending, start: LocalDate?, end: LocalDate = LocalDate.now()): Balance {
        if (start != null && end < start) {
            throw IllegalArgumentException("Start date must come before end date!")
        }
        var `break` = false
        var definitely = 0f
        var maybe = 0f
        var targetDefinitely = 0f
        var targetMaybe = 0f
        var balance = 0f
        var targetBalance = 0f
        var occurrenceCount = 0
        val spendingEvents = mutableListOf<SpendingEvent>()
        var movingStart = spending.fromStartDate
        var movingEnd = spending.fromEndDate
        var counter = 0
        do {
            if (start == null || movingEnd >= start) {
                val target = spending.target?.let { if (it > 0) -it.toFloat() else it.toFloat() }
                        ?: spending.value.toFloat()
                if (end >= movingStart) {
                    if (spending.enabled) {
                        maybe += spending.value.toFloat()
                        targetMaybe += target
                    }
                    if (end > movingEnd) {
                        if (spending.enabled) {
                            definitely += spending.value.toFloat()
                            targetDefinitely += target
                            balance += spending.value.toFloat()
                            targetBalance += target
                        }
                    }
                    spendingEvents.add(SpendingEvent(movingStart, movingEnd, definitely, maybe))
                } else {
                    `break` = true
                }
            }
            counter++
            movingStart = spending.fromStartDate + (spending.cycleMultiplier * counter * spending.cycle)
            movingEnd = spending.fromEndDate + (spending.cycleMultiplier * counter * spending.cycle)
            spending.occurrenceCount?.let {
                if (++occurrenceCount >= it) {
                    `break` = true
                }
            }
        } while (!`break`)
        return Balance(definitely, maybe, balance, targetDefinitely, targetMaybe, targetBalance, spendingEvents.toTypedArray())
    }
}
