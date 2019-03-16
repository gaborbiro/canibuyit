package com.gb.canibuyit.feature.monzo.api

import com.gb.canibuyit.feature.monzo.api.model.ApiMonzoLogin
import io.reactivex.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface MonzoAuthApi {

    @FormUrlEncoded
    @POST("/oauth2/token")
    fun login(@Field("grant_type") grantType: String,
              @Field("code") code: String,
              @Field("redirect_uri") redirectUri: String,
              @Field("client_id") clientId: String,
              @Field("client_secret") clientSecret: String): Single<ApiMonzoLogin>

    @FormUrlEncoded
    @POST("/oauth2/token")
    fun refresh(@Field("grant_type") grantType: String,
                @Field("refresh_token") refreshToken: String,
                @Field("client_id") clientId: String,
                @Field("client_secret") clientSecret: String): Single<ApiMonzoLogin>
}