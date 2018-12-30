package com.gb.canibuyit.repository

import com.gb.canibuyit.db.Contract
import com.gb.canibuyit.db.model.ApiSpending
import com.gb.canibuyit.db.model.ApiSpentByCycle
import com.gb.canibuyit.model.CycleSpent
import com.gb.canibuyit.model.Spending
import com.gb.canibuyit.util.doIfBoth
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.stmt.DeleteBuilder
import io.reactivex.Completable
import io.reactivex.Single
import java.sql.SQLException
import javax.inject.Inject

class SpentByCycleRepository @Inject
constructor(private val spentByCycleDao: Dao<ApiSpentByCycle, Int>,
            private val spendingDao: Dao<ApiSpending, Int>) {

    fun saveSpentByCycles(spendings: List<Spending>): Completable {
        return Completable.create { emitter ->
            try {
                spendings.forEach { newSpending ->
                    val apiSpending = spendingDao.queryForId(newSpending.id)
                    val foreignCollection = apiSpending.spentByByCycle
                    val items = newSpending.spentByCycle
                    Pair(foreignCollection, items).doIfBoth { (foreignCollection, items) ->
                        foreignCollection.clear()
                        foreignCollection.addAll(items.map { spentByCycle ->
                            ApiSpentByCycle(
                                    spentByCycle.id,
                                    apiSpending,
                                    spentByCycle.from,
                                    spentByCycle.to,
                                    spentByCycle.amount,
                                    spentByCycle.count,
                                    spentByCycle.enabled)
                        }.asIterable())
                    }
                }
                emitter.onComplete()
            } catch (e: SQLException) {
                emitter.onError(Exception("Error creating/updating spentByCycles", e))
            }
        }
    }

    fun deleteSpendByCycleBySpendingId(spendingId: Int): Completable {
        return Completable.create { emitter ->
            try {
                val builder: DeleteBuilder<ApiSpentByCycle, Int> = spentByCycleDao.deleteBuilder()
                builder.where()
                        .eq(Contract.SpentByCycle.SPENDING, spendingId)
                spentByCycleDao.delete(builder.prepare())
                spendingDao.queryForId(spendingId).spentByByCycle?.refreshCollection()
                emitter.onComplete()
            } catch (e: SQLException) {
                emitter.onError(e)
            }
        }
    }

    fun setSpentByCycleEnabled(cycleSpent: CycleSpent, enabled: Boolean): Single<Boolean> {
        return Single.create {
            try {
                val spending = spendingDao.queryForId(cycleSpent.spendingId)
                spending.spentByByCycle?.find { it.id == cycleSpent.id }?.copy(enabled = enabled)
                        .run {
                            spending.spentByByCycle?.update(this)
                        }
                it.onSuccess(spentByCycleDao.queryForId(cycleSpent.id).enabled!!)
            } catch (e: SQLException) {
                it.onError(e)
            }
        }
    }
}