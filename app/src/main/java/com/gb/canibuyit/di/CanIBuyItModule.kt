package com.gb.canibuyit.di

import android.app.Application
import android.content.Context
import com.gb.canibuyit.db.SpendingDBHelper
import com.gb.canibuyit.db.model.ApiProject
import com.gb.canibuyit.db.model.ApiSpending
import com.gb.canibuyit.rx.SchedulerProvider
import com.gb.canibuyit.rx.SchedulerProviderImpl
import com.j256.ormlite.dao.Dao
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(includes = arrayOf(MonzoApiModule::class))
class CanIBuyItModule {

    @Provides
    fun provideContext(application: Application): Context = application.applicationContext

    @Provides
    @Singleton
    fun provideSchedulerProvider(schedulerProvider: SchedulerProviderImpl): SchedulerProvider =
        schedulerProvider

    @Singleton
    @Provides
    fun provideProjectDao(spendingDBHelper: SpendingDBHelper): Dao<ApiProject, Int> {
        return spendingDBHelper.getDao<Dao<ApiProject, Int>, ApiProject>(ApiProject::class.java)
    }

    @Singleton
    @Provides
    fun provideSpendingDao(spendingDBHelper: SpendingDBHelper): Dao<ApiSpending, Int> {
        return spendingDBHelper.getDao<Dao<ApiSpending, Int>, ApiSpending>(ApiSpending::class.java)
    }
}
