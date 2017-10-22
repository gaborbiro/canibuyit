package com.gb.canibuythat.api.model

class ApiLogin(
        val access_token: String,
        val client_id: String,
        val expires_in: Int,
        val refresh_token: String,
        val token_type: String,
        val user_id: String
)
