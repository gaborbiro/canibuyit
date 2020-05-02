package com.gb.canibuyit.feature.monzo.model

import com.gb.canibuyit.feature.spending.persistence.model.DBSpending
import java.time.LocalDateTime

class Transaction(val amount: Int,
                  val created: LocalDateTime,
                  val description: String?,
                  val id: String,
                  val category: DBSpending.Category,
                  val originalCategory: String) {

    override fun toString(): String {
        return "Transaction(category='$category', amount=$amount, created=$created, description='$description', id='$id', originalCategory='$originalCategory')"
    }
}