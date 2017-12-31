package com.gb.canibuythat.di

import android.app.Application
import android.content.Context
import com.gb.canibuythat.db.SpendingDBHelper
import com.gb.canibuythat.db.model.ApiProject
import com.gb.canibuythat.db.model.ApiSaving
import com.gb.canibuythat.db.model.ApiSpending
import com.gb.canibuythat.rx.SchedulerProvider
import com.gb.canibuythat.rx.SchedulerProviderImpl
import com.j256.ormlite.dao.Dao
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

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

    @Provides
    fun provideProjectDao(spendingDBHelper: SpendingDBHelper): Dao<ApiProject, Int> {
        return spendingDBHelper.getDao<Dao<ApiProject, Int>, ApiProject>(ApiProject::class.java)
    }

    @Provides
    fun provideSpendingDao(spendingDBHelper: SpendingDBHelper): Dao<ApiSpending, Int> {
        return spendingDBHelper.getDao<Dao<ApiSpending, Int>, ApiSpending>(ApiSpending::class.java)
    }

    @Provides
    fun provideSavingsDao(spendingDBHelper: SpendingDBHelper): Dao<ApiSaving, Int> {
        return spendingDBHelper.getDao<Dao<ApiSaving, Int>, ApiSaving>(ApiSaving::class.java)
    }
}
