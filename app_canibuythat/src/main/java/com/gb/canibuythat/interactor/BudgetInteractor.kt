package com.gb.canibuythat.interactor

import android.content.Context
import android.os.Environment
import com.gb.canibuythat.exception.DomainException
import com.gb.canibuythat.model.Balance
import com.gb.canibuythat.model.BudgetItem
import com.gb.canibuythat.provider.BudgetDbHelper
import com.gb.canibuythat.provider.BudgetProvider
import com.gb.canibuythat.repository.BudgetRepository
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

class BudgetInteractor @Inject
constructor(private val budgetRepository: BudgetRepository, private val appContext: Context, private val schedulerProvider: SchedulerProvider) {

    val all: Maybe<List<BudgetItem>>
        get() = budgetRepository.all
                .onErrorResumeNext { throwable: Throwable -> Maybe.error<List<BudgetItem>>(DomainException("Error loading from database. See logs.", throwable)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())

    fun createOrUpdate(budgetItem: BudgetItem): Single<Dao.CreateOrUpdateStatus> {
        return budgetRepository.createOrUpdate(budgetItem)
                .onErrorResumeNext { throwable -> Single.error<Dao.CreateOrUpdateStatus>(DomainException("Error updating budget item in database. See logs.", throwable)) }
                .doOnSuccess { appContext.contentResolver.notifyChange(BudgetProvider.BUDGET_ITEMS_URI, null) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun createOrUpdateMonzoCategories(budgetItems: List<BudgetItem>): Completable {
        return budgetRepository.createOrUpdateMonzoCategories(budgetItems)
                .onErrorResumeNext { throwable -> Completable.error(DomainException("Error updating monzo cache. See logs.", throwable)) }
                .doOnComplete { appContext.contentResolver.notifyChange(BudgetProvider.BUDGET_ITEMS_URI, null) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun delete(id: Int): Completable {
        return budgetRepository.delete(id)
                .onErrorResumeNext { Completable.error(DomainException("Error deleting budget item $id in database. See logs.")) }
                .doOnComplete { appContext.contentResolver.notifyChange(BudgetProvider.BUDGET_ITEMS_URI, null) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun read(id: Int): Maybe<BudgetItem> {
        return budgetRepository.read(id)
                .onErrorResumeNext { throwable: Throwable -> Maybe.error<BudgetItem>(DomainException("Error reading budget item $id from database. See logs.", throwable)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun calculateBalance(): Single<Balance> {
        return budgetRepository.calculateBalance()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun importDatabase(file: String): Completable {
        return budgetRepository.importDatabaseFromFile(file)
                .doOnComplete { appContext.contentResolver.notifyChange(BudgetProvider.BUDGET_ITEMS_URI, null) }
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
            val targetFilename = "budget-" + sdf.format(Date()) + ".sqlite"
            val to = File(targetFolder, targetFilename)

            val data = Environment.getDataDirectory()
            val currentDBPath = "/data/" + pack + "/databases/" + BudgetDbHelper.DATABASE_NAME
            val from = File(data, currentDBPath)

            FileUtils.copyFiles(from, to)
            return Completable.complete()
        } catch (t: Throwable) {
            return Completable.error(DomainException("Error exporting database", t))
        }

    }
}
