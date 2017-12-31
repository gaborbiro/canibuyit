package com.gb.canibuythat.model

import java.util.*

class Saving(val spendingId: Int,
             val amount: Double,
             val created: Date,
             val target: Double) {
    override fun toString(): String {
        return "Saving(spendingId=$spendingId, amount=$amount, created=$created, target=$target)"
    }
}