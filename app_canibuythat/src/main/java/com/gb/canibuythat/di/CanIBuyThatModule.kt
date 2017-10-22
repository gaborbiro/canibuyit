package com.gb.canibuythat.di

import android.app.Application
import android.content.Context

import com.gb.canibuythat.db.SpendingDBHelper
import com.gb.canibuythat.model.Spending
import com.gb.canibuythat.rx.SchedulerProvider
import com.gb.canibuythat.rx.SchedulerProviderImpl
import com.j256.ormlite.dao.Dao

import javax.inject.Singleton

import dagger.Module
import dagger.Provides

@Module(includes = arrayOf(MonzoApiModule::class))
class CanIBuyThatModule {

    @Provides
    fun provideContext(application: Application): Context {
        return application.applicationContext
    }

    @Provides
    @Singleton
    fun provideSchedulerProvider(schedulerProvider: SchedulerProviderImpl): SchedulerProvider {
        return schedulerProvider
    }
}
