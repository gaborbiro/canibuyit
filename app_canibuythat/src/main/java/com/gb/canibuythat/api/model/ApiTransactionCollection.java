package com.gb.canibuythat.api.model;

import android.support.annotation.Keep;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;

@Keep
@Getter
public class ApiTransactionCollection {

    @SerializedName("transactions")
    private ApiTransaction[] transactions;
}
