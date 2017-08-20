package com.gb.canibuythat.exception;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.HttpException;

public class DomainException extends Throwable {

    private ResponseBody responseBody;
    private int code;
    private Kind kind;
    private Action action;

    public DomainException(Throwable raw) {
        this(raw.getMessage(), raw);
    }

    public DomainException(String message) {
        this(message, null);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
        this.code = -1;
        this.kind = Kind.GENERIC;

        if (cause instanceof HttpException) {
            this.kind = Kind.HTTP;
            this.code = ((HttpException) cause).code();

            if (code == 401) {
                action = Action.LOGIN;
            }

            this.responseBody = ((HttpException) cause).response().errorBody();
        }
        if (cause instanceof IOException) {
            this.kind = Kind.NETWORK;
        }
    }

    public ResponseBody getResponseBody() {
        return responseBody;
    }

    public int getCode() {
        return code;
    }

    public Kind getKind() {
        return kind;
    }

    public Action getAction() {
        return action;
    }

    public enum Kind {
        NETWORK,
        HTTP,
        GENERIC;
    }

    public enum Action {
        LOGIN
    }
}
