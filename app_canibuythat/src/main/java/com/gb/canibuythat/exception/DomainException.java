package com.gb.canibuythat.exception;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.HttpException;

public class DomainException extends Throwable {

    private ResponseBody responseBody;
    private int code;
    private DomainException.KIND kind;

    public DomainException(Throwable raw) {
        this(raw.getMessage(), raw.getCause());
    }

    public DomainException(String message) {
        this(message, null);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
        this.code = -1;
        this.kind = DomainException.KIND.GENERIC;

        if (cause instanceof HttpException) {
            this.kind = DomainException.KIND.HTTP;
            this.code = ((HttpException) cause).code();
            this.responseBody = ((HttpException) cause).response().errorBody();
        }
        if (cause instanceof IOException) {
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
