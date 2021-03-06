package com.gb.canibuyit.di

import android.app.Application
import android.content.Context
import com.gb.canibuyit.base.rx.SchedulerProvider
import com.gb.canibuyit.base.rx.SchedulerProviderImpl
import com.gb.canibuyit.feature.dispatch.di.DispatchModule
import com.gb.canibuyit.feature.monzo.di.MonzoModule
import com.gb.canibuyit.feature.project.model.ApiProject
import com.gb.canibuyit.feature.spending.persistence.SpendingDBHelper
import com.gb.canibuyit.feature.spending.persistence.model.DBSpending
import com.gb.canibuyit.util.LocalDateSerializer
import com.gb.prefsutil.PrefsUtil
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
    fun provideSpendingDao(spendingDBHelper: SpendingDBHelper): Dao<DBSpending, Int> {
        return spendingDBHelper.getDao<Dao<DBSpending, Int>, DBSpending>(DBSpending::class.java)
    }

    @Provides
    @Singleton
    fun providePrefsUtil(appContext: Context): PrefsUtil = PrefsUtil(appContext, "settings")

    @Provides
    @Singleton
    internal fun provideGson(): Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        .registerTypeAdapter(LocalDate::class.java, LocalDateSerializer)
        .enableComplexMapKeySerialization()
        .create()

    @Provides
    @Singleton
    internal fun provideGsonConverterFactory(gson: Gson): GsonConverterFactory =
        GsonConverterFactory.create(gson)
}
