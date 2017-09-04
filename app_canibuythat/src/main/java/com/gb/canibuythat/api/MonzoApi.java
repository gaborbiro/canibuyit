package com.gb.canibuythat.api;

import com.gb.canibuythat.api.model.ApiAccountCollection;
import com.gb.canibuythat.api.model.ApiLogin;
import com.gb.canibuythat.api.model.ApiTransactionCollection;

import io.reactivex.Single;
import okhttp3.RequestBody;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface MonzoApi {

    @Multipart
    @POST("/oauth2/token")
    Single<ApiLogin> login(@Part("grant_type") RequestBody grantType,
                           @Part("code") RequestBody code,
                           @Part("redirect_uri") RequestBody redirectUri,
                           @Part("client_id") RequestBody clientId,
                           @Part("client_secret") RequestBody clientSecret);

    @Multipart
    @POST("/oauth2/token")
    Single<ApiLogin> refresh(@Part("grant_type") RequestBody grantType,
                             @Part("refresh_token") RequestBody refreshToken,
                             @Part("client_id") RequestBody clientId,
                             @Part("client_secret") RequestBody clientSecret);

    @GET("/transactions")
    Single<ApiTransactionCollection> transactions(@Query("account_id") String accountId);
}
