package com.gb.canibuyit.feature.monzo.api.model

class ApiMonzoLogin(
    val access_token: String,
    val client_id: String,
    val expires_in: Int,
    val refresh_token: String,
    val token_type: String,
    val user_id: String
)
