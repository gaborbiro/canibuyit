package com.gb.canibuyit.feature.dispatch.di

import com.gb.canibuyit.BuildConfig
import com.gb.canibuyit.DEFAULT_TIMEOUT_SECONDS
import com.gb.canibuyit.feature.dispatch.MONZO_DISPATCH_API_BASE
import com.gb.canibuyit.feature.dispatch.api.DispatchApi
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
class DispatchModule {

    @Provides
    @Singleton
    internal fun provideMonzoDispatchApi(gsonConverterFactory: GsonConverterFactory): DispatchApi {
        val okHttpClientBuilder = OkHttpClient.Builder()
            .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val interceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            okHttpClientBuilder.addInterceptor(interceptor)
        }

        return Retrofit.Builder()
            .client(okHttpClientBuilder.build())
            .baseUrl(MONZO_DISPATCH_API_BASE)
            .addConverterFactory(gsonConverterFactory)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(DispatchApi::class.java)
    }
}