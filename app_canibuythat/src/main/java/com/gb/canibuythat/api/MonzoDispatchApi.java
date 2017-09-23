package com.gb.canibuythat.api;

import com.gb.canibuythat.api.model.ApiDispatchRegistration;

import io.reactivex.Single;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface MonzoDispatchApi {

    @POST("/register")
    Single<ApiDispatchRegistration> register(@Query("token") String token);
}
