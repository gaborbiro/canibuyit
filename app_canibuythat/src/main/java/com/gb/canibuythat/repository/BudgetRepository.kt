package com.gb.canibuythat.repository

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import com.gb.canibuythat.UserPreferences
import com.gb.canibuythat.model.Balance
import com.gb.canibuythat.model.BudgetItem
import com.gb.canibuythat.provider.BalanceCalculator
import com.gb.canibuythat.provider.BudgetDbHelper
import com.gb.canibuythat.provider.Contract
import com.j256.ormlite.dao.Dao
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import java.sql.SQLException
import javax.inject.Inject

class BudgetRepository @Inject
constructor(private val budgetDbHelper: BudgetDbHelper, private val userPreferences: UserPreferences) {
    private val budgetItemDao: Dao<BudgetItem, Int> = budgetDbHelper.getDao<Dao<BudgetItem, Int>, BudgetItem>(BudgetItem::class.java)

    val all: Maybe<List<BudgetItem>>
        get() {
            return Maybe.create<List<BudgetItem>> { emitter ->
                try {
                    emitter.onSuccess(budgetItemDao.queryForAll())
                } catch (e: SQLException) {
                    emitter.onError(e)
                }
            }
        }

    fun createOrUpdate(budgetItem: BudgetItem): Single<Dao.CreateOrUpdateStatus> {
        return Single.create { emitter ->
            try {
                emitter.onSuccess(budgetItemDao.createOrUpdate(budgetItem))
            } catch (e: SQLException) {
                emitter.onError(e)
            }
        }
    }

    fun createOrUpdateMonzoCategories(budgetItems: List<BudgetItem>): Completable {
        return Completable.create { emitter ->
            val savedBudgetItems: List<BudgetItem> = budgetItemDao.queryForAll()
                    .filter { it.sourceData[BudgetItem.SOURCE_MONZO_CATEGORY] != null }
                    .sortedBy { it.sourceData[BudgetItem.SOURCE_MONZO_CATEGORY] }

            try {
                budgetItems.forEach {
                    val category: String? = it.sourceData[BudgetItem.SOURCE_MONZO_CATEGORY]

                    if (category == null) {
                        emitter.onError(Exception("BudgetItem '" + it.name + "' has no category"))
                    } else {
                        val index = savedBudgetItems.indexOfFirst { it.sourceData[BudgetItem.SOURCE_MONZO_CATEGORY] == category }

                        if (index >= 0) {
                            it.id = savedBudgetItems[index].id
                            it.notes = savedBudgetItems[index].notes
                            budgetItemDao.update(it)
                        } else {
                            budgetItemDao.create(it)
                        }
                    }
                }
                emitter.onComplete()
            } catch (e: SQLException) {
                emitter.onError(Exception("Error creating/updating monzo categories", e))
            }
        }
    }

    fun delete(id: Int): Completable {
        return Completable.create { emitter ->
            try {
                if (budgetItemDao.deleteById(id) > 0) {
                    emitter.onComplete()
                } else {
                    emitter.onError(Exception("Delete error: budget item $id was not found in the database"))
                }
            } catch (e: SQLException) {
                emitter.onError(e)
            }
        }
    }

    fun read(id: Int): Maybe<BudgetItem> {
        return Maybe.create<BudgetItem> { emitter ->
            try {
                val budgetItem = budgetItemDao.queryForId(id)

                if (budgetItem != null) {
                    emitter.onSuccess(budgetItem)
                } else {
                    emitter.onComplete()
                }
            } catch (e: SQLException) {
                emitter.onError(e)
            }
        }
    }

    fun calculateBalance(): Single<Balance> {
        var bestCase = 0f
        var worstCase = 0f

        // blocking thread
        val balanceReading = userPreferences.balanceReading

        for (item in budgetItemDao) {
            if (item.enabled) {
                val startDate = balanceReading?.`when`
                val result = BalanceCalculator.get()
                        .getEstimatedBalance(item, startDate, userPreferences.estimateDate)
                bestCase += result.bestCase
                worstCase += result.worstCase
            }
        }

        if (balanceReading != null) {
            bestCase += balanceReading.balance
            worstCase += balanceReading.balance
        }
        return Single.just(Balance(balanceReading, bestCase, worstCase))
    }

    fun importDatabaseFromFile(file: String): Completable {
        val db: SQLiteDatabase
        val cursor: Cursor
        try {
            db = budgetDbHelper.getDatabaseFromFile(file)
        } catch (e: SQLiteException) {
            return Completable.error(Exception("Cannot open database from " + file, e))
        }
        try {
            cursor = budgetDbHelper.getAllBudgetItems(db)
        } catch (e: SQLException) {
            return Completable.error(Exception("Error reading " + file, e))
        }
        try {
            budgetDbHelper.replaceBudgetDatabase(cursor)
            return Completable.complete()
        } catch (e: SQLException) {
            return Completable.error(Exception("Error writing to table " + Contract.BudgetItem.TABLE, e))
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
