package com.gb.canibuyit.feature.dispatch.api;

import com.gb.canibuyit.feature.dispatch.api.model.ApiDispatchRegistration;

import io.reactivex.Single;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface DispatchApi {

    @POST("/register")
    Single<ApiDispatchRegistration> register(@Query("token") String token);
}
