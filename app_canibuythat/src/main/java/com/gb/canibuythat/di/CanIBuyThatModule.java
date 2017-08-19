package com.gb.canibuythat.di;

import android.app.Application;
import android.content.Context;

import com.gb.canibuythat.App;
import com.gb.canibuythat.rx.SchedulerProvider;
import com.gb.canibuythat.rx.SchedulerProviderImpl;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(includes = MonzoApiModule.class)
public class CanIBuyThatModule {

    @Provides
    public Context provideContext(Application application) {
        return application.getApplicationContext();
    }

    @Provides
    public App provideApp(Application application) {
        return (App) application;
    }

    @Provides
    @Singleton
    public SchedulerProvider provideSchedulerProvider(SchedulerProviderImpl schedulerProvider) {
        return schedulerProvider;
    }
}
