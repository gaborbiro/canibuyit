package com.gb.canibuythat.model

import com.gb.canibuythat.db.model.ApiSpending
import java.time.ZonedDateTime

class Transaction(val amount: Int,
                  val created: ZonedDateTime,
                  val description: String?,
                  val id: String,
                  val category: ApiSpending.Category) {

    override fun toString(): String {
        return "Transaction(category='$category', amount=$amount, created=$created, description='$description', id='$id')"
    }
}