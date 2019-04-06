package com.gb.canibuyit.feature.backup.data

import android.content.Context
import android.os.Environment
import com.gb.canibuyit.error.DomainException
import com.gb.canibuyit.feature.spending.data.SpendingInteractor
import com.gb.canibuyit.feature.spending.persistence.DATABASE_NAME
import com.gb.canibuyit.base.rx.SchedulerProvider
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

    private fun prepareImportCompletable(completable: Completable): Completable {
        return completable
                .onErrorResumeNext { throwable ->
                    Completable.error(
                            DomainException("Error importing database. See logs.", throwable))
                }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun exportSpendings(targetFilename: String): Completable {
        val finalFromPath = targetFilename.let { if (!it.endsWith("sqlite")) "$it.sqlite" else it }
        val currentDBPath =
            "/data/${appContext.packageName}/databases/$DATABASE_NAME"

        return try {
            exportFile(finalFromPath, currentDBPath)
            Completable.complete()
        } catch (t: Throwable) {
            Completable.error(DomainException("Error exporting database", t))
        }
    }

    private fun exportFile(toPath: String, fromPath: String) {
        val from = File(Environment.getDataDirectory(), fromPath)
        val to = File(toPath)
        FileUtils.copyFiles(from, to)
    }
}