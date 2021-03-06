package com.gb.canibuyit.feature.spending.data

import com.gb.canibuyit.feature.spending.model.Spending
import java.time.LocalDate

data class Balance(
    var amount: Float = 0f,
    /**
     * Use the spending target instead of the historical average-based estimation. The "well behaved user" scenario.
     * Not all spendings have a target set, in which case this defaults back to the estimate
     */
    var target: Float = 0f,
    var spendingEvents: Array<SpendingEvent>? = emptyArray(),
    var spending: Spending?
)

class SpendingEvent(val start: LocalDate,
                    val end: LocalDate,
                    val amount: Float,
                    val total: Float) {
    override fun toString(): String {
        return "SpendingEvent(start=$start, end=$end, amount=$amount)"
    }
}