package com.gb.canibuyit.feature.spending.model

import java.math.BigDecimal
import java.time.LocalDate

class Saving(val id: Int?,
             val spendingId: Int?,
             val amount: BigDecimal,
             val created: LocalDate,
             val target: Int) {

    override fun toString(): String {
        return "Saving(spendingId=$spendingId, amount=$amount, created=$created, target=$target)"
    }
}