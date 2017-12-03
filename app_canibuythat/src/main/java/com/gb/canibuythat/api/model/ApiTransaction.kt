package com.gb.canibuythat.api.model

class ApiTransaction(
        val amount: Int,
        val created: String,
        val description: String,
        val id: String,
        val notes: String,
        val category: String,
        val include_in_spending: Boolean,
        val is_load: Boolean) {
    override fun toString(): String {
        return "ApiTransaction(category='$category', amount=$amount, created='$created', description='$description', notes='$notes', include_in_spending=$include_in_spending, id='$id', is_load=$is_load)"
    }
}