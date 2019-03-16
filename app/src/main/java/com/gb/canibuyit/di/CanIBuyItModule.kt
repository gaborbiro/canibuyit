package com.gb.canibuyit.di

import android.app.Application
import android.content.Context
import com.gb.canibuyit.feature.spending.persistence.SpendingDBHelper
import com.gb.canibuyit.feature.project.model.ApiProject
import com.gb.canibuyit.feature.spending.persistence.model.ApiSpending
import com.gb.canibuyit.feature.dispatch.di.DispatchModule
import com.gb.canibuyit.feature.monzo.di.MonzoModule
import com.gb.canibuyit.rx.SchedulerProvider
import com.gb.canibuyit.rx.SchedulerProviderImpl
import com.gb.canibuyit.util.localDateSerializer
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.j256.ormlite.dao.Dao
import dagger.Module
import dagger.Provides
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import javax.inject.Singleton

@Module(includes = [MonzoModule::class, DispatchModule::class])
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
