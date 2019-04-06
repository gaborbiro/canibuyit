package com.gb.canibuyit.feature.monzo.api

import com.gb.canibuyit.feature.monzo.api.model.ApiMonzoTransactions
import com.gb.canibuyit.feature.monzo.api.model.ApiWebhooks

import io.reactivex.Completable
import io.reactivex.Single
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MonzoApi {

    @GET("/transactions")
    fun transactions(@Query("account_id") accountId: String, @Query("since") since: String? = null,
                     @Query("before") before: String? = null): Single<ApiMonzoTransactions>

    @FormUrlEncoded
    @POST("/webhooks")
    fun registerWebhook(@Field("account_id") accountId: String,
                        @Field("url") url: String): Completable

    @GET("/webhooks")
    fun getWebhooks(@Query("account_id") accountId: String): Single<ApiWebhooks>

    @DELETE("/webhooks/{id}")
    fun deleteWebhook(@Path("id") webHookId: String): Completable
}
