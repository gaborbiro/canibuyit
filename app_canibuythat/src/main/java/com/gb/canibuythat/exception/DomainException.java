package com.gb.canibuythat.exception;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.HttpException;

public class DomainException extends Throwable {

    private ResponseBody responseBody;
    private int code;
    private DomainException.KIND kind;

    public DomainException(Throwable raw) {
        super(raw.getMessage());
        this.code = -1;
        this.kind = DomainException.KIND.GENERIC;

        if (raw instanceof HttpException) {
            this.kind = DomainException.KIND.HTTP;
            this.code = ((HttpException) raw).code();
            this.responseBody = ((HttpException) raw).response().errorBody();
        }
        if (raw instanceof IOException) {
            this.kind = DomainException.KIND.NETWORK;
        }
    }

    public ResponseBody getResponseBody() {
        return responseBody;
    }

    public int getCode() {
        return code;
    }

    public KIND getKind() {
        return kind;
    }

    public enum KIND {
        NETWORK,
        HTTP,
        GENERIC;
    }
}
