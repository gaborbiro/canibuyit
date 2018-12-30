package com.gb.canibuyit.repository

import com.gb.canibuyit.db.model.ApiSaving
import com.gb.canibuyit.db.model.ApiSpending
import com.gb.canibuyit.model.Spending
import com.j256.ormlite.dao.Dao
import io.reactivex.Completable
import java.sql.SQLException
import javax.inject.Inject

class SavingsRepository @Inject
constructor(private val savingsDao: Dao<ApiSaving, Int>,
            private val spendingDao: Dao<ApiSpending, Int>) {

    fun saveSavings(spendings: List<Spending>): Completable {
        return Completable.create { emitter ->
            try {
                savingsDao.deleteBuilder().delete()
                spendings.flatMap { it.savings?.asList() ?: emptyList() }.forEach { saving ->
                    val apiSpending = spendingDao.queryForId(saving.spendingId)
                    savingsDao.create(ApiSaving(
                            saving.id,
                            apiSpending,
                            saving.amount,
                            saving.created,
                            saving.target))
                }
                emitter.onComplete()
            } catch (e: SQLException) {
                emitter.onError(Exception("Error creating/updating savings", e))
            }
        }
    }
}