package com.gb.canibuyit.repository

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import com.gb.canibuyit.db.Contract
import com.gb.canibuyit.db.SpendingDBHelper
import io.reactivex.Completable
import java.sql.SQLException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupingRepository @Inject
constructor(private val spendingDBHelper: SpendingDBHelper) {

    fun importAllSpendings(file: String): Completable {
        return importSpendings(file, null)
    }

    fun importMonzoSpendings(file: String): Completable {
        return importSpendings(file, SpendingDBHelper.FILTER_MONZO)
    }

    fun importNonMonzoSpendings(file: String): Completable {
        return importSpendings(file, SpendingDBHelper.FILTER_NON_MONZO)
    }

    fun importSpendings(file: String, filter: Map<String, Any?>?): Completable {
        val db: SQLiteDatabase
        val cursor: Cursor
        try {
            db = spendingDBHelper.getDatabaseFromFile(file)
        } catch (e: SQLiteException) {
            return Completable.error(Exception("Cannot open database from $file", e))
        }
        try {
            cursor = spendingDBHelper.getAllSpendings(db)
        } catch (e: SQLException) {
            return Completable.error(Exception("Error reading $file", e))
        }
        try {
            spendingDBHelper.insertSpendings(cursor, filter)
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