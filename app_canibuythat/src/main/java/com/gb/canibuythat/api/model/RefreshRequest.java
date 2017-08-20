package com.gb.canibuythat.api.model;

import com.google.gson.annotations.SerializedName;

import lombok.Builder;

@Builder
public class RefreshRequest {
    @SerializedName("grant_type")
    private String grantType;

    @SerializedName("client_id")
    private String clientId;

    @SerializedName("client_secret")
    private String clientSecret;

    @SerializedName("refresh_token")
    private String refreshToken;
}
