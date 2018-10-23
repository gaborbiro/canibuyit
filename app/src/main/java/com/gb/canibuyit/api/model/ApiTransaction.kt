package com.gb.canibuyit.api.model

class ApiTransaction(
    val amount: Int,
    val created: String,
    val description: String,
    val id: String,
    val notes: String,
    val category: String,
    val decline_reason: String?) {
    override fun toString(): String {
        return "ApiTransaction(category='$category', amount=$amount, created='$created', description='$description', notes='$notes', decline_reason=$decline_reason, id='$id')"
    }
}