package com.gb.canibuythat.api.model;

import android.support.annotation.Keep;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;

@Keep
@Getter
public class ApiAccountsResponse {
    @SerializedName("accounts")
    private ApiAccount[] accounts;
}
