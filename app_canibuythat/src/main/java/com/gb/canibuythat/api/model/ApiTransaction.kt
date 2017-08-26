package com.gb.canibuythat.api.model

class ApiTransaction(
        val account_balance: Int,
        val amount: Int,
        val created: String,
        val currency: String,
        val description: String,
        val id: String,
        val merchant: String,
        val notes: String,
        val is_load: Boolean,
        val settled: String,
        val category: String
)