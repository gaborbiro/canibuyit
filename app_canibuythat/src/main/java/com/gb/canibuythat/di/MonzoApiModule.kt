package com.gb.canibuythat.di

import com.gb.canibuythat.AppConstants
import com.gb.canibuythat.BuildConfig
import com.gb.canibuythat.MonzoConstants
import com.gb.canibuythat.api.MonzoApi
import com.gb.canibuythat.api.MonzoAuthApi
import com.gb.canibuythat.api.MonzoAuthenticator
import com.gb.canibuythat.api.MonzoDispatchApi
import com.google.gson.Gson

import java.util.concurrent.TimeUnit

import javax.inject.Singleton

import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

@Module
class MonzoApiModule {

    @Provides
    @Singleton
    internal fun provideMonzoAuthApi(gsonConverterFactory: GsonConverterFactory): MonzoAuthApi {
        val okHttpClientBuilder = OkHttpClient.Builder()
                .readTimeout(AppConstants.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .connectTimeout(AppConstants.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            okHttpClientBuilder.addInterceptor(interceptor)
        }

        return Retrofit.Builder()
                .client(okHttpClientBuilder.build())
                .baseUrl(MonzoConstants.MONZO_API_BASE)
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
                .readTimeout(AppConstants.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .connectTimeout(AppConstants.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .addInterceptor(monzoAuthenticator)
                .authenticator(monzoAuthenticator)

        if (BuildConfig.DEBUG) {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            okHttpClientBuilder.addInterceptor(interceptor)
        }

        return Retrofit.Builder()
                .client(okHttpClientBuilder.build())
                .baseUrl(MonzoConstants.MONZO_API_BASE)
                .addConverterFactory(gsonConverterFactory)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(MonzoApi::class.java)
    }

    @Provides
    @Singleton
    internal fun provideMonzoDispatchApi(gsonConverterFactory: GsonConverterFactory): MonzoDispatchApi {
        val okHttpClientBuilder = OkHttpClient.Builder()
                .readTimeout(AppConstants.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .connectTimeout(AppConstants.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            okHttpClientBuilder.addInterceptor(interceptor)
        }

        return Retrofit.Builder()
                .client(okHttpClientBuilder.build())
                .baseUrl(MonzoConstants.MONZO_DISPATCH_API_BASE)
                .addConverterFactory(gsonConverterFactory)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(MonzoDispatchApi::class.java)
    }

    @Provides
    @Singleton
    internal fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    internal fun provideGsonConverterFactory(gson: Gson): GsonConverterFactory {
        return GsonConverterFactory.create(gson)
    }
}
