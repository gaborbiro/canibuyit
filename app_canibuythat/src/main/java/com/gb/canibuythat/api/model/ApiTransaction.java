package com.gb.canibuythat.api.model;

import android.support.annotation.Keep;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;

@Keep
@Getter
public class ApiTransaction {

    @SerializedName("account_balance")
    private int accountBalance;

    @SerializedName("amount")
    private int amount;

    @SerializedName("created")
    private String created;

    @SerializedName("currency")
    private String currency;

    @SerializedName("description")
    private String description;

    @SerializedName("id")
    private String id;

    @SerializedName("merchant")
    private String merchant;

    @SerializedName("notes")
    private String notes;

    @SerializedName("is_load")
    private boolean isLoad;

    @SerializedName("settled")
    private String settled;

    @SerializedName("category")
    private String category;
}
