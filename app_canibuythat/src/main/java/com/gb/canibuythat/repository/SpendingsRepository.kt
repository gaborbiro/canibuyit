package com.gb.canibuythat.repository

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import com.gb.canibuythat.UserPreferences
import com.gb.canibuythat.model.Balance
import com.gb.canibuythat.model.Spending
import com.gb.canibuythat.provider.BalanceCalculator
import com.gb.canibuythat.provider.SpendingDbHelper
import com.gb.canibuythat.provider.Contract
import com.j256.ormlite.dao.Dao
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import java.sql.SQLException
import javax.inject.Inject

class SpendingsRepository @Inject
constructor(private val spendingDbHelper: SpendingDbHelper, private val userPreferences: UserPreferences) {
    private val spendingDao: Dao<Spending, Int> = spendingDbHelper.getDao<Dao<Spending, Int>, Spending>(Spending::class.java)

    val all: Maybe<List<Spending>>
        get() {
            return Maybe.create<List<Spending>> { emitter ->
                try {
                    emitter.onSuccess(spendingDao.queryForAll())
                } catch (e: SQLException) {
                    emitter.onError(e)
                }
            }
        }

    fun createOrUpdate(spending: Spending): Single<Dao.CreateOrUpdateStatus> {
        return Single.create { emitter ->
            try {
                emitter.onSuccess(spendingDao.createOrUpdate(spending))
            } catch (e: SQLException) {
                emitter.onError(e)
            }
        }
    }

    fun createOrUpdateMonzoCategories(spendings: List<Spending>): Completable {
        return Completable.create { emitter ->
            val savedSpendings: List<Spending> = spendingDao.queryForAll()
                    .filter { it.sourceData[Spending.SOURCE_MONZO_CATEGORY] != null }
                    .sortedBy { it.sourceData[Spending.SOURCE_MONZO_CATEGORY] }

            try {
                spendings.forEach {
                    val category: String? = it.sourceData[Spending.SOURCE_MONZO_CATEGORY]

                    if (category == null) {
                        emitter.onError(Exception("Spending '" + it.name + "' has no category"))
                    } else {
                        val index = savedSpendings.indexOfFirst { it.sourceData[Spending.SOURCE_MONZO_CATEGORY] == category }

                        if (index >= 0) {
                            it.id = savedSpendings[index].id
                            it.notes = savedSpendings[index].notes
                            it.target = savedSpendings[index].target
                            it.cycle = savedSpendings[index].cycle
                            it.cycleMultiplier = savedSpendings[index].cycleMultiplier

                            if (savedSpendings[index].average != 0.0) {
                                it.average = savedSpendings[index].average
                            }
                            spendingDao.update(it)
                        } else {
                            spendingDao.create(it)
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
                if (spendingDao.deleteById(id) > 0) {
                    emitter.onComplete()
                } else {
                    emitter.onError(Exception("Delete error: spending $id was not found in the database"))
                }
            } catch (e: SQLException) {
                emitter.onError(e)
            }
        }
    }

    fun read(id: Int): Maybe<Spending> {
        return Maybe.create<Spending> { emitter ->
            try {
                val spending = spendingDao.queryForId(id)

                if (spending != null) {
                    emitter.onSuccess(spending)
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

        for (spending in spendingDao) {
            if (spending.enabled) {
                val startDate = balanceReading?.`when`
                val result = BalanceCalculator.getEstimatedBalance(spending, startDate, userPreferences.estimateDate)
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
