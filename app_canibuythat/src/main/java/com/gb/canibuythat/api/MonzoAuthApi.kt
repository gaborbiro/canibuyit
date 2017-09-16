package com.gb.canibuythat.api

import com.gb.canibuythat.api.model.ApiLogin
import io.reactivex.Single
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface MonzoAuthApi {
    @Multipart
    @POST("/oauth2/token")
    fun login(@Part("grant_type") grantType: RequestBody,
              @Part("code") code: RequestBody,
              @Part("redirect_uri") redirectUri: RequestBody,
              @Part("client_id") clientId: RequestBody,
              @Part("client_secret") clientSecret: RequestBody): Single<ApiLogin>

    @Multipart
    @POST("/oauth2/token")
    fun refresh(@Part("grant_type") grantType: RequestBody,
                @Part("refresh_token") refreshToken: RequestBody,
                @Part("client_id") clientId: RequestBody,
                @Part("client_secret") clientSecret: RequestBody): Single<ApiLogin>
}