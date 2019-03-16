package com.gb.canibuyit.feature.monzo.api.model;

import com.google.gson.annotations.SerializedName;

public class MonzoErrorResponse {
    @SerializedName("code")
    private String code;

    @SerializedName("error")
    private String error;

    @SerializedName("error_description")
    private String errorDescription;

    @SerializedName("message")
    private String message;

    public String getCode() {
        return code;
    }

    public String getError() {
        return error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public String getMessage() {
        return message;
    }
}
