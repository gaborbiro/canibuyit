package com.gb.canibuythat.interactor

import android.content.Context
import android.os.Environment
import com.gb.canibuythat.exception.DomainException
import com.gb.canibuythat.model.Balance
import com.gb.canibuythat.model.Spending
import com.gb.canibuythat.provider.SpendingDbHelper
import com.gb.canibuythat.provider.SpendingProvider
import com.gb.canibuythat.repository.SpendingsRepository
import com.gb.canibuythat.rx.SchedulerProvider
import com.gb.canibuythat.util.FileUtils
import com.j256.ormlite.dao.Dao
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class SpendingInteractor @Inject
constructor(private val spendingsRepository: SpendingsRepository, private val appContext: Context, private val schedulerProvider: SchedulerProvider) {

    val all: Maybe<List<Spending>>
        get() = spendingsRepository.all
                .onErrorResumeNext { throwable: Throwable -> Maybe.error<List<Spending>>(DomainException("Error loading from database. See logs.", throwable)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())

    fun createOrUpdate(spending: Spending): Single<Dao.CreateOrUpdateStatus> {
        return spendingsRepository.createOrUpdate(spending)
                .onErrorResumeNext { throwable -> Single.error<Dao.CreateOrUpdateStatus>(DomainException("Error updating spending in database. See logs.", throwable)) }
                .doOnSuccess { appContext.contentResolver.notifyChange(SpendingProvider.SPENDINGS_URI, null) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun createOrUpdateMonzoCategories(spendings: List<Spending>): Completable {
        return spendingsRepository.createOrUpdateMonzoCategories(spendings)
                .onErrorResumeNext { throwable -> Completable.error(DomainException("Error updating monzo cache. See logs.", throwable)) }
                .doOnComplete { appContext.contentResolver.notifyChange(SpendingProvider.SPENDINGS_URI, null) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun delete(id: Int): Completable {
        return spendingsRepository.delete(id)
                .onErrorResumeNext { Completable.error(DomainException("Error deleting spending $id in database. See logs.")) }
                .doOnComplete { appContext.contentResolver.notifyChange(SpendingProvider.SPENDINGS_URI, null) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun read(id: Int): Maybe<Spending> {
        return spendingsRepository.read(id)
                .onErrorResumeNext { throwable: Throwable -> Maybe.error<Spending>(DomainException("Error reading spending $id from database. See logs.", throwable)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun calculateBalance(): Single<Balance> {
        return spendingsRepository.calculateBalance()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun importDatabase(file: String): Completable {
        return spendingsRepository.importDatabaseFromFile(file)
                .doOnComplete { appContext.contentResolver.notifyChange(SpendingProvider.SPENDINGS_URI, null) }
                .onErrorResumeNext { throwable -> Completable.error(DomainException("Error importing database. See logs.", throwable)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun exportDatabase(): Completable {
        try {
            val pack = appContext.packageName

            val sd = Environment.getExternalStorageDirectory()
            val targetFolder = File(sd.toString() + "/CanIBuyThat")

            if (!targetFolder.exists()) {
                targetFolder.mkdirs()
            }
            val sdf = SimpleDateFormat("yyyyMMdd'T'HHmmssZ")
            val targetFilename = "spendings-" + sdf.format(Date()) + ".sqlite"
            val to = File(targetFolder, targetFilename)

            val data = Environment.getDataDirectory()
            val currentDBPath = "/data/" + pack + "/databases/" + SpendingDbHelper.DATABASE_NAME
            val from = File(data, currentDBPath)

            FileUtils.copyFiles(from, to)
            return Completable.complete()
        } catch (t: Throwable) {
            return Completable.error(DomainException("Error exporting database", t))
        }

    }
}
