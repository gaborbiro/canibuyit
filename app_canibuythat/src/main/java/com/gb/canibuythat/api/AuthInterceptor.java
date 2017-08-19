package com.gb.canibuythat.api;

import android.support.annotation.NonNull;

import com.gb.canibuythat.CredentialsProvider;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    public static final String AUTHORIZATION = "Authorization";
    public static final String HEADER_VALUE_PREFIX = "Bearer ";

    private final CredentialsProvider credentialsProvider;

    public AuthInterceptor(CredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        return chain.proceed(chain.request().newBuilder()
                .addHeader(AUTHORIZATION, HEADER_VALUE_PREFIX + credentialsProvider.getAccessToken())
                .build());
    }
}
