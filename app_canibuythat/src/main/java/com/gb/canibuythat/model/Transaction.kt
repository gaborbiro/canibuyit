package com.gb.canibuythat.model

import org.threeten.bp.ZonedDateTime

class Transaction(val amount: Double,
                  val created: ZonedDateTime,
                  val currency: String,
                  val description: String?,
                  val id: String,
                  val merchant: String?,
                  val notes: String?,
                  val isLoad: Boolean?,
                  val settled: ZonedDateTime?,
                  val category: String) {

    override fun toString(): String {
        return "Transaction(amount=$amount, created=$created, currency='$currency', description=$description, id='$id', merchant=$merchant, notes=$notes, isLoad=$isLoad, settled=$settled, category='$category')"
    }
}