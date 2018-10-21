package com.gb.canibuyit.interactor

import android.content.Context
import android.os.Environment
import com.gb.canibuyit.db.SpendingDBHelper
import com.gb.canibuyit.exception.DomainException
import com.gb.canibuyit.repository.BackupingRepository
import com.gb.canibuyit.rx.SchedulerProvider
import com.gb.canibuyit.util.FileUtils
import io.reactivex.Completable
import java.io.File
import javax.inject.Inject

class BackupingInteractor @Inject
constructor(private val backupingRepository: BackupingRepository,
            private val appContext: Context,
            private val schedulerProvider: SchedulerProvider,
            private val spendingInteractor: SpendingInteractor) {

    fun importAllSpendings(file: String): Completable {
        spendingInteractor.clearSpendings()
        return prepareImportCompletable(backupingRepository.importAllSpendings(file))
    }

    fun importMonzoSpendings(file: String): Completable {
        return prepareImportCompletable(backupingRepository.importMonzoSpendings(file))
    }

    fun importNonMonzoSpendings(file: String): Completable {
        return prepareImportCompletable(backupingRepository.importNonMonzoSpendings(file))
    }

    fun showPickerForExport() {

    }

    private fun prepareImportCompletable(completable: Completable): Completable {
        return completable
                .onErrorResumeNext { throwable -> Completable.error(DomainException("Error importing database. See logs.", throwable)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun exportSpendings(targetFilename: String): Completable {
        try {
            val pack = appContext.packageName

            var to = File(targetFilename)

            if (!to.extension.equals("sqlite")) {
                to = File(targetFilename + ".sqlite")
            }

            val data = Environment.getDataDirectory()
            val currentDBPath = "/data/" + pack + "/databases/" + SpendingDBHelper.DATABASE_NAME
            val from = File(data, currentDBPath)

            FileUtils.copyFiles(from, to)
            return Completable.complete()
        } catch (t: Throwable) {
            return Completable.error(DomainException("Error exporting database", t))
        }
    }
}