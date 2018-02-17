package com.gb.canibuythat.repository

import com.gb.canibuythat.db.Contract
import com.gb.canibuythat.db.model.ApiSaving
import com.gb.canibuythat.db.model.ApiSpending
import com.gb.canibuythat.model.Spending
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.stmt.DeleteBuilder
import io.reactivex.Completable
import java.sql.SQLException
import javax.inject.Inject

class SavingsRepository @Inject
constructor(private val savingsDao: Dao<ApiSaving, Int>,
            private val spendingDao: Dao<ApiSpending, Int>) {

    fun create(spendings: List<Spending>): Completable {
        return Completable.create { emitter ->
            try {
                spendings.forEach {
                    it.savings?.let {
                        it.forEach {
                            savingsDao.create(ApiSaving(
                                    it.id,
                                    spendingDao.queryForId(it.spendingId),
                                    it.amount,
                                    it.created,
                                    it.target))
                        }
                    }
                }
                emitter.onComplete()
            } catch (e: SQLException) {
                emitter.onError(Exception("Error creating/updating savings", e))
            }
        }
    }

    private fun deleteSavingsBySpending(spendingId: Int): Completable {
        return Completable.create { emitter ->
            try {
                val builder: DeleteBuilder<ApiSaving, Int> = savingsDao.deleteBuilder()
                builder.where().eq(Contract.Savings.SPENDING, spendingDao.queryForId(spendingId))
                savingsDao.delete(builder.prepare())
                emitter.onComplete()
            } catch (e: SQLException) {
                emitter.onError(e)
            }
        }
    }

    fun clearAll(): Completable {
        return Completable.defer({
            try {
                savingsDao.deleteBuilder().delete()
                Completable.complete()
            } catch (e: SQLException) {
                Completable.error(e)
            }
        })
    }
}