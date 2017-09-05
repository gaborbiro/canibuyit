package com.gb.canibuythat.repository

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import com.gb.canibuythat.provider.Contract
import com.gb.canibuythat.provider.SpendingDbHelper
import io.reactivex.Completable
import java.sql.SQLException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupingRepository @Inject
constructor(private val spendingDbHelper: SpendingDbHelper) {

    fun importDatabaseFromFile(file: String): Completable {
        val db: SQLiteDatabase
        val cursor: Cursor
        try {
            db = spendingDbHelper.getDatabaseFromFile(file)
        } catch (e: SQLiteException) {
            return Completable.error(Exception("Cannot open database from " + file, e))
        }
        try {
            cursor = spendingDbHelper.getAllSpendings(db)
        } catch (e: SQLException) {
            return Completable.error(Exception("Error reading " + file, e))
        }
        try {
            spendingDbHelper.replaceSpendingDatabase(cursor)
            return Completable.complete()
        } catch (e: SQLException) {
            return Completable.error(Exception("Error writing to table " + Contract.Spending.TABLE, e))
        } finally {
            try {
                cursor.close()
            } catch (t: Throwable) {
                // ignore
            }
            try {
                db.close()
            } catch (t: Throwable) {
                // ignore
            }
        }
    }
}