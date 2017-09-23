package com.gb.canibuythat.api

import com.gb.canibuythat.api.model.ApiLogin
import io.reactivex.Single
import okhttp3.RequestBody
import retrofit2.http.*

interface MonzoAuthApi {

    @Multipart
    @POST("/oauth2/token")
    fun login(@Part("grant_type") grantType: RequestBody,
              @Part("code") code: RequestBody,
              @Part("redirect_uri") redirectUri: RequestBody,
              @Part("client_id") clientId: RequestBody,
              @Part("client_secret") clientSecret: RequestBody): Single<ApiLogin>

    @FormUrlEncoded
    @POST("/oauth2/token")
    fun refresh(@Field("grant_type") grantType: String,
                @Field("refresh_token") refreshToken: String,
                @Field("client_id") clientId: String,
                @Field("client_secret") clientSecret: String): Single<ApiLogin>
}