package com.gb.canibuyit.feature.monzo.di

import com.gb.canibuyit.BuildConfig
import com.gb.canibuyit.DEFAULT_TIMEOUT_SECONDS
import com.gb.canibuyit.feature.monzo.MONZO_API_BASE
import com.gb.canibuyit.feature.monzo.api.MonzoApi
import com.gb.canibuyit.feature.monzo.api.MonzoAuthApi
import com.gb.canibuyit.feature.monzo.api.MonzoAuthenticator
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
class MonzoModule {

    @Provides
    @Singleton
    internal fun provideMonzoAuthApi(gsonConverterFactory: GsonConverterFactory): MonzoAuthApi {
        val okHttpClientBuilder = OkHttpClient.Builder()
                .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            okHttpClientBuilder.addInterceptor(interceptor)
        }

        return Retrofit.Builder()
                .client(okHttpClientBuilder.build())
                .baseUrl(MONZO_API_BASE)
                .addConverterFactory(gsonConverterFactory)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(MonzoAuthApi::class.java)
    }

    @Provides
    @Singleton
    internal fun provideMonzoApi(
        gsonConverterFactory: GsonConverterFactory,
        monzoAuthenticator: MonzoAuthenticator): MonzoApi {

        val okHttpClientBuilder = OkHttpClient.Builder()
                .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .addInterceptor(monzoAuthenticator)
                .authenticator(monzoAuthenticator)

        if (BuildConfig.DEBUG) {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            okHttpClientBuilder.addInterceptor(interceptor)
        }

        return Retrofit.Builder()
                .client(okHttpClientBuilder.build())
                .baseUrl(MONZO_API_BASE)
                .addConverterFactory(gsonConverterFactory)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(MonzoApi::class.java)
    }
}
