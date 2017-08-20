package com.gb.canibuythat.api.model;

import android.support.annotation.Keep;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;

@Keep
@Getter
public class ApiAccount {
    @SerializedName("id")
    private String id;

    @SerializedName("description")
    private String description;

    @SerializedName("created")
    private String created;
}
