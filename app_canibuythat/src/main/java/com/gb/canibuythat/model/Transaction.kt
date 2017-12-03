package com.gb.canibuythat.model

import org.threeten.bp.ZonedDateTime

class Transaction(val amount: Int,
                  val created: ZonedDateTime,
                  val description: String?,
                  val id: String,
                  val category: Spending.Category) {

    override fun toString(): String {
        return "Transaction(category='$category', amount=$amount, created=$created, description=$description, id='$id')"
    }
}