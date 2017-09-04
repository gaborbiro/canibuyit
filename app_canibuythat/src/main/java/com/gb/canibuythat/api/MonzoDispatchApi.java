package com.gb.canibuythat.api;

import retrofit2.http.POST;
import retrofit2.http.Query;

public interface MonzoDispatchApi {

    @POST("/register")
    void register(@Query("name") String name, @Query("token") String token);
}
