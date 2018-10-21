package com.gb.canibuyit.api;

import com.gb.canibuyit.api.model.ApiDispatchRegistration;

import io.reactivex.Single;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface MonzoDispatchApi {

    @POST("/register")
    Single<ApiDispatchRegistration> register(@Query("token") String token);
}
