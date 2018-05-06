package com.gb.canibuythat.model

import java.time.LocalDate

data class Balance(
        /**
         * We know for certain that this amount has been spent by the selected date
         */
        var definitely: Float,
        /**
         * `maybeEvenThisMuch` is `definitely` plus spendings that may have been spent by the selected date.
         * A weekly spending for eg. "may be spent" for one week after which it becomes "definitely spent"
         * and is included only with the `definitely` sum.
         */
        var maybeEvenThisMuch: Float,
        var balance: Float,
        /**
         * Use the spending target instead of the historical average-based estimation. The "well behaved user" scenario.
         * Not all spendings have a target set, in which case this defaults back to the estimate
         */
        var targetDefinitely: Float,
        /**
         * Similar to maybeEvenThisMuch
         */
        var targetMaybeEvenThisMuch: Float,
        var targetBalance: Float,
        var spendingEvents: Array<SpendingEvent>?)

class SpendingEvent(val start: LocalDate,
                    val end: LocalDate,
                    val definitely: Float,
                    val maybe: Float)