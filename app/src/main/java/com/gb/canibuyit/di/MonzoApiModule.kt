package com.gb.canibuyit.di

import com.gb.canibuyit.BuildConfig
import com.gb.canibuyit.DEFAULT_TIMEOUT_SECONDS
import com.gb.canibuyit.MONZO_API_BASE
import com.gb.canibuyit.MONZO_DISPATCH_API_BASE
import com.gb.canibuyit.api.MonzoApi
import com.gb.canibuyit.api.MonzoAuthApi
import com.gb.canibuyit.api.MonzoAuthenticator
import com.gb.canibuyit.api.MonzoDispatchApi
import com.gb.canibuyit.util.localDateSerializer
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
class MonzoApiModule {

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

    @Provides
    @Singleton
    internal fun provideMonzoDispatchApi(gsonConverterFactory: GsonConverterFactory): MonzoDispatchApi {
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
                .baseUrl(MONZO_DISPATCH_API_BASE)
                .addConverterFactory(gsonConverterFactory)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(MonzoDispatchApi::class.java)
    }

    @Provides
    @Singleton
    internal fun provideGson(): Gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
            .registerTypeAdapter(LocalDate::class.java, localDateSerializer)
            .enableComplexMapKeySerialization()
            .create()

    @Provides
    @Singleton
    internal fun provideGsonConverterFactory(gson: Gson): GsonConverterFactory =
        GsonConverterFactory.create(gson)
}
