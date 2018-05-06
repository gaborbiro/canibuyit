package com.gb.canibuythat.model

import java.time.LocalDate

data class Balance(
        /**
         * We know for certain that this amount has been spent by the selected date
         */
        var definitely: Float = 0f,
        /**
         * `maybeEvenThisMuch` is `definitely` plus spendings that may have been spent by the selected date.
         * A weekly spending for eg. "may be spent" for one week after which it becomes "definitely spent"
         * and is included only with the `definitely` sum.
         */
        var maybeEvenThisMuch: Float = 0f,
        var balance: Float = 0f,
        /**
         * Use the spending target instead of the historical average-based estimation. The "well behaved user" scenario.
         * Not all spendings have a target set, in which case this defaults back to the estimate
         */
        var targetDefinitely: Float = 0f,
        /**
         * Similar to maybeEvenThisMuch
         */
        var targetMaybeEvenThisMuch: Float = 0f,
        var targetBalance: Float = 0f,
        var spendingEvents: Array<SpendingEvent>? = emptyArray())

class SpendingEvent(val start: LocalDate,
                    val end: LocalDate,
                    val definitely: Float,
                    val maybe: Float)