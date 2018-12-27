package com.gb.canibuyit.repository

import com.gb.canibuyit.db.Contract
import com.gb.canibuyit.db.model.ApiSpending
import com.gb.canibuyit.db.model.ApiSpentByCycle
import com.gb.canibuyit.model.CycleSpent
import com.gb.canibuyit.model.Spending
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.stmt.DeleteBuilder
import io.reactivex.Completable
import io.reactivex.Single
import java.sql.SQLException
import javax.inject.Inject

class SpentByCycleRepository @Inject
constructor(private val spentByCycleDao: Dao<ApiSpentByCycle, Int>,
            private val spendingDao: Dao<ApiSpending, Int>) {

    fun create(spendings: List<Spending>): Completable {
        return Completable.create { emitter ->
            try {
                spendings.flatMap { it.spentByCycle ?: emptyList() }
                        .forEach { cycleSpent: CycleSpent ->
                            spentByCycleDao.create(ApiSpentByCycle(
                                    cycleSpent.id,
                                    spendingDao.queryForId(cycleSpent.spendingId),
                                    cycleSpent.from,
                                    cycleSpent.to,
                                    cycleSpent.amount,
                                    cycleSpent.count,
                                    cycleSpent.enabled))
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
                        .eq(Contract.SpentByCycle.SPENDING, spendingDao.queryForId(spendingId))
                spentByCycleDao.delete(builder.prepare())
                emitter.onComplete()
            } catch (e: SQLException) {
                emitter.onError(e)
            }
        }
    }

    fun clearAll(): Completable {
        return Completable.defer {
            try {
                spentByCycleDao.deleteBuilder().delete()
                Completable.complete()
            } catch (e: SQLException) {
                Completable.error(e)
            }
        }
    }

    fun setSpentByCycleEnabled(cycleSpent: CycleSpent, enabled: Boolean): Single<Boolean> {
        return Single.create {
            try {
                val spending = spendingDao.queryForId(cycleSpent.spendingId)
                spentByCycleDao.update(ApiSpentByCycle(
                        id = cycleSpent.id,
                        spending = spending,
                        from = cycleSpent.from,
                        to = cycleSpent.to,
                        amount = cycleSpent.amount,
                        count = cycleSpent.count,
                        enabled = enabled))
                it.onSuccess(spentByCycleDao.queryForId(cycleSpent.id).enabled!!)
            } catch (e: SQLException) {
                it.onError(e)
            }
        }
    }
}