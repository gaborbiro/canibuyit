package com.gb.canibuythat.interactor

import android.content.Context
import android.os.Environment
import com.gb.canibuythat.exception.DomainException
import com.gb.canibuythat.provider.SpendingDbHelper
import com.gb.canibuythat.provider.SpendingProvider
import com.gb.canibuythat.repository.BackupingRepository
import com.gb.canibuythat.rx.SchedulerProvider
import com.gb.canibuythat.util.FileUtils
import io.reactivex.Completable
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class BackupingInteractor @Inject
constructor(private val backupingRepository: BackupingRepository, private val appContext: Context, private val schedulerProvider: SchedulerProvider) {

    fun importAllSpendings(file: String): Completable {
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
                .doOnComplete { appContext.contentResolver.notifyChange(SpendingProvider.SPENDINGS_URI, null) }
                .onErrorResumeNext { throwable -> Completable.error(DomainException("Error importing database. See logs.", throwable)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun exportSpendings(): Completable {
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