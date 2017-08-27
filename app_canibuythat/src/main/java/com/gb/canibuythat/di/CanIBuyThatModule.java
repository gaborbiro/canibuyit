package com.gb.canibuythat.di;

import android.app.Application;
import android.content.Context;

import com.gb.canibuythat.provider.BudgetDbHelper;
import com.gb.canibuythat.rx.SchedulerProvider;
import com.gb.canibuythat.rx.SchedulerProviderImpl;
import com.j256.ormlite.android.apptools.OpenHelperManager;

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
    @Singleton
    public SchedulerProvider provideSchedulerProvider(SchedulerProviderImpl schedulerProvider) {
        return schedulerProvider;
    }
}
