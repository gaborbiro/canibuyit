package com.gb.canibuythat.api.model

import android.support.annotation.Keep

import com.gb.canibuythat.model.Login
import com.google.gson.annotations.SerializedName

import lombok.Getter

class ApiLogin(
        val access_token: String,
        val client_id: String,
        val expires_in: String,
        val refresh_token: String,
        val token_type: String,
        val user_id: String
)
