package com.gb.canibuythat.di;

import com.gb.canibuythat.AppConstants;
import com.gb.canibuythat.BuildConfig;
import com.gb.canibuythat.CredentialsProvider;
import com.gb.canibuythat.MonzoConstants;
import com.gb.canibuythat.api.AuthInterceptor;
import com.gb.canibuythat.api.MonzoApi;
import com.google.gson.Gson;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
public class MonzoApiModule {

    @Provides
    @Singleton
    MonzoApi provideMonzoApi(
            CredentialsProvider credentialsProvider,
            GsonConverterFactory gsonConverterFactory) {

        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
                .readTimeout(AppConstants.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .connectTimeout(AppConstants.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor(credentialsProvider));

        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            okHttpClientBuilder.addInterceptor(interceptor);
        }

        return new Retrofit.Builder()
                .client(okHttpClientBuilder.build())
                .baseUrl(MonzoConstants.MONZO_API_BASE)
                .addConverterFactory(gsonConverterFactory)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(MonzoApi.class);
    }

    @Provides
    @Singleton
    Gson provideGson() {
        return new Gson();
    }

    @Provides
    @Singleton
    GsonConverterFactory provideGsonConverterFactory(Gson gson) {
        return GsonConverterFactory.create(gson);
    }
}
