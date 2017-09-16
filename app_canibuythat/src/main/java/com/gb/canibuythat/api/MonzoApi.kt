package com.gb.canibuythat.api

import com.gb.canibuythat.api.model.ApiLogin
import com.gb.canibuythat.api.model.ApiTransactionCollection
import com.gb.canibuythat.api.model.ApiWebhooks

import io.reactivex.Completable
import io.reactivex.Single
import okhttp3.RequestBody
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface MonzoApi {

    @GET("/transactions")
    fun transactions(@Query("account_id") accountId: String): Single<ApiTransactionCollection>

    @FormUrlEncoded
    @POST("/webhooks")
    fun registerWebhook(@Field("account_id") accountId: String,
                        @Field("url") url: String): Completable

    @GET("/webhooks")
    fun getWebhooks(@Query("account_id") accountId: String): Single<ApiWebhooks>

    @DELETE("/webhooks/{id}")
    fun deleteWebhook(@Path("id") webhookId: String): Completable
}
