package com.gb.canibuyit.feature.monzo.api.model

class ApiMonzoTransactions(val transactions: Array<ApiMonzoTransaction>)

class ApiMonzoTransaction(
    val amount: Int,
    val created: String,
    val settled: String?,
    val description: String,
    val id: String,
    val notes: String,
    val category: String,
    val decline_reason: String?) {

    override fun toString(): String {
        return "ApiMonzoTransaction(category='$category', amount=$amount, created='$created', description='$description', notes='$notes', decline_reason=$decline_reason, id='$id')"
    }
}