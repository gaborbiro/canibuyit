package com.gb.canibuythat.api;

import com.gb.canibuythat.api.model.ApiLogin;

import io.reactivex.Single;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface MonzoApi {

    @POST("/oauth2/token")
    Single<ApiLogin> login(@Query("grant_type") String grantType, @Query("client_id") String clientId,
                           @Query("client_secret") String clientSecret, @Query("redirect_uri") String redirectUri,
                           @Query("code") String authorizationCode);
}
