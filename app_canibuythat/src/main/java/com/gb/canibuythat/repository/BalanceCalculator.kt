package com.gb.canibuythat.repository

import com.gb.canibuythat.model.Spending
import com.gb.canibuythat.util.DateUtils
import com.gb.canibuythat.util.clearLowerBits
import com.gb.canibuythat.util.toCalendar
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
    fun getEstimatedBalance(spending: Spending, start: Date?, end: Date?): BalanceResult {
        if (start != null && end != null && end.before(start)) {
            throw IllegalArgumentException("Start date must come before end date!")
        }

        var `break` = false
        var definitely = 0f
        var maybe = 0f
        var targetDefinitely = 0f
        var targetMaybe = 0f
        var occurrenceCount = 0
        val movingStart = spending.fromStartDate.toCalendar()
        val movingEnd = spending.fromEndDate.toCalendar()
        val start = start?.clearLowerBits()
        val end: Calendar = end?.toCalendar()?.clearLowerBits() ?: clearLowerBits()
        do {
            if (start == null || movingEnd.timeInMillis >= start.time) {
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
                } else {
                    `break` = true
                }
            }
            spending.cycle!!.apply(movingStart, spending.cycleMultiplier!!)
            spending.cycle!!.apply(movingEnd, spending.cycleMultiplier!!)
            if (spending.occurrenceCount != null && ++occurrenceCount >= spending.occurrenceCount!!) {
                `break` = true
            }
        } while (!`break`)
        return BalanceResult(definitely, maybe, targetDefinitely, targetMaybe, null/*spendingEvents.toTypedArray()*/)
    }

    data class BalanceResult(
            /**
             * We know for certain that this amount has been spent by the selected date
             */
            val definitely: Float,
            /**
             * `maybeEvenThisMuch` is `definitely` plus spendings that may have been spent by the selected date.
             * A weekly spending for eg. "may be spent" for one week after which it becomes "definitely spent"
             * and is included only with the `definitely` sum.
             */
            val maybeEvenThisMuch: Float,
            /**
             * Use the spending target instead of the historical average-based estimation. The "well behaved user" scenario.
             * Not all spendings have a target set, in which case this defaults back to the estimate
             */
            val targetDefinitely: Float,
            /**
             * Similar to maybeEvenThisMuch
             */
            val targetMaybeEvenThisMuch: Float,
            /**
             * Spending event is the latest day on which we expect a payment/income to happen.
             * For eg if a weekly Spending starts on a Monday, than the array of spending
             * events will be all the Sundays after that Monday and before today (including
             * today).
             */
            val spendingEvents: Array<Date>?)
}
