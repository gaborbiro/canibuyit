package com.gb.canibuyit.model

import java.time.LocalDate

data class Balance(
        var amount: Float = 0f,
        /**
         * Use the spending target instead of the historical average-based estimation. The "well behaved user" scenario.
         * Not all spendings have a target set, in which case this defaults back to the estimate
         */
        var target: Float = 0f,
        var spendingEvents: Array<SpendingEvent>? = emptyArray())

class SpendingEvent(val start: LocalDate,
                    val end: LocalDate,
                    val amount: Float)