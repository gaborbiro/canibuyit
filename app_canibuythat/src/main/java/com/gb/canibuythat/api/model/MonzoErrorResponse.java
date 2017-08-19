package com.gb.canibuythat.api.model;

import com.google.gson.annotations.SerializedName;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class MonzoErrorResponse {
    @SerializedName("code")
    private String code;

    @SerializedName("error")
    private String error;

    @SerializedName("error_description")
    private String errorDescription;

    @SerializedName("message")
    private String message;
}
