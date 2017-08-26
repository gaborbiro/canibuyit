package com.gb.canibuythat.exception;

import com.gb.canibuythat.api.model.MonzoErrorResponse;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.converter.gson.GsonConverterFactory;

public class MonzoException extends DomainException {

    private MonzoErrorResponse monzoError;

    public MonzoException(Throwable raw) {
        super(raw);

        if (getResponseBody() != null) {
            try {
                monzoError = getErrorResponseConverter().convert(getResponseBody());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getMessage() {
        if (monzoError != null) {
            return monzoError.getMessage();
        } else {
            return "Error communicating with Monzo!";
        }
    }

    private static Converter<ResponseBody, MonzoErrorResponse> getErrorResponseConverter() {
        return (Converter<ResponseBody, MonzoErrorResponse>) GsonConverterFactory.create(new Gson()).responseBodyConverter(MonzoErrorResponse.class, null, null);
    }
}
