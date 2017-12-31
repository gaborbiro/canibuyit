package com.gb.canibuythat.repository

import com.gb.canibuythat.db.Contract
import com.gb.canibuythat.db.model.ApiSaving
import com.gb.canibuythat.db.model.ApiSpending
import com.gb.canibuythat.exception.DomainException
import com.gb.canibuythat.model.Saving
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.stmt.DeleteBuilder
import io.reactivex.Completable
import io.reactivex.Single
import java.sql.SQLException
import javax.inject.Inject

class SavingsRepository @Inject
constructor(private val savingsDao: Dao<ApiSaving, Int>,
            private val spendingDao: Dao<ApiSpending, Int>,
            private val savingMapper: SavingMapper) {

    fun createIfNotExist(savings: Array<Saving>): Completable {
        return Completable.create { emitter ->
            try {
                savings.forEach {
                    savingsDao.createIfNotExists(ApiSaving(
                            it.id,
                            spendingDao.queryForId(it.spendingId),
                            it.amount,
                            it.created,
                            it.target))
                }
                emitter.onComplete()
            } catch (e: SQLException) {
                emitter.onError(DomainException("Error persisting savings", e))
            }
        }
    }

    fun getAll(): Array<Saving> {
        return savingsDao.queryForAll().map(savingMapper::mapApiSaving).toTypedArray()
    }

    fun getSavingsForSpending(spendingId: Int): Single<Array<Saving>> {
        return Single.create { emitter ->
            try {
                val builder = savingsDao.queryBuilder().where().eq(Contract.Savings.SPENDING, spendingDao.queryForId(spendingId))
                emitter.onSuccess(savingsDao.query(builder.prepare()).map(savingMapper::mapApiSaving).toTypedArray())
            } catch (e: SQLException) {
                emitter.onError(e)
            }
        }
    }

    fun deleteSavingsForSpending(spendingId: Int): Completable {
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

    fun clearAll(): Single<Int> {
        return Single.defer({
            try {
                Single.just(savingsDao.deleteBuilder().delete())
            } catch (e: SQLException) {
                Single.error<Int>(e)
            }
        })
    }
}