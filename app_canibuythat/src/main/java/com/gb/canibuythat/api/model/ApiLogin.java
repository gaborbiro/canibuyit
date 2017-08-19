package com.gb.canibuythat.api.model;

import android.support.annotation.Keep;

import com.gb.canibuythat.model.Login;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;

@Keep
@Getter
public class ApiLogin {

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("client_id")
    private String clientId;

    @SerializedName("expires_in")
    private String expiresIn;

    @SerializedName("refresh_token")
    private String refreshToken;

    @SerializedName("token_type")
    private String tokenType;

    @SerializedName("user_id")
    private String userId;
}
