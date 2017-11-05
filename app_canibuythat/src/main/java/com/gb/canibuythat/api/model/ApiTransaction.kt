package com.gb.canibuythat.api.model

class ApiTransaction(
        val amount: Int,
        val created: String,
        val currency: String,
        val description: String,
        val id: String,
        val notes: String,
        val is_load: Boolean,
        val settled: String,
        val decline_reason: String,
        val category: String,
        val include_in_spending: Boolean
)