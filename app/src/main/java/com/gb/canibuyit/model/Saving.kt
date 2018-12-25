package com.gb.canibuyit.model

import java.time.LocalDate

class Saving(val id: Int?,
             val spendingId: Int?,
             val amount: Double,
             val created: LocalDate,
             val target: Int) {
    override fun toString(): String {
        return "Saving(spendingId=$spendingId, amount=$amount, created=$created, target=$target)"
    }
}