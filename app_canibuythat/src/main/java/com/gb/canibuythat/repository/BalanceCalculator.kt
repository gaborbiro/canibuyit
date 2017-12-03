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

     * @param endDate if null, `today` is used instead
     * *
     * @return two floating point values, the first is the minimum possible value, the second is
     * * the maximum possible value
     */
    fun getEstimatedBalance(spending: Spending, startDate: Date?, endDate: Date?): BalanceResult {
        if (startDate != null && endDate != null && endDate.before(startDate)) {
            throw IllegalArgumentException("Start date must come before end date!")
        }

        var `break` = false
        var definitelySpentThisMuch = 0f
        var maybeSpentThisMuch = 0f
        var targetDefinitelySpentThisMuch = 0f
        var targetMaybeSpentThisMuch = 0f
        var count = 0
        val occurrenceStart = spending.fromStartDate.toCalendar()
        val occurrenceEnd = spending.fromEndDate.toCalendar()
        val start = startDate?.clearLowerBits()
        val end: Calendar = endDate?.toCalendar() ?: Calendar.getInstance()
        end.clearLowerBits()
        do {
            if (start == null || occurrenceEnd.timeInMillis >= start.time) {
                val r = DateUtils.compare(end, occurrenceStart, occurrenceEnd)
                val target = spending.target?.let { if (it > 0) -it.toFloat() else it.toFloat() } ?: spending.value.toFloat()
                if (r >= -1) { // >= start date
                    if (spending.enabled) {
                        maybeSpentThisMuch += spending.value.toFloat()
                        targetMaybeSpentThisMuch += target
                    }
                    if (r > 1) { // > end date
                        if (spending.enabled) {
                            definitelySpentThisMuch += spending.value.toFloat()
                            targetDefinitelySpentThisMuch += target
                        }
                    }
                } else {
                    `break` = true
                }
            }
            spending.cycle!!.apply(occurrenceStart, spending.cycleMultiplier!!)
            spending.cycle!!.apply(occurrenceEnd, spending.cycleMultiplier!!)
            if (spending.occurrenceCount != null && ++count >= spending.occurrenceCount!!) {
                `break` = true
            }
        } while (!`break`)
        return BalanceResult(definitelySpentThisMuch, maybeSpentThisMuch, targetDefinitelySpentThisMuch, targetMaybeSpentThisMuch, null/*spendingEvents.toTypedArray()*/)
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
