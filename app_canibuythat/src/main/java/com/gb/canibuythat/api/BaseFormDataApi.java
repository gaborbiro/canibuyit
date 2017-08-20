package com.gb.canibuythat.api;

import okhttp3.MediaType;
import okhttp3.RequestBody;

public abstract class BaseFormDataApi {

    protected RequestBody text(String value) {
        return RequestBody.create(MediaType.parse("text/plain"), value);
    }
}
