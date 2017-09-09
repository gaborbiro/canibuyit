package com.gb.canibuythat.repository

import com.gb.canibuythat.UserPreferences
import com.gb.canibuythat.model.Balance
import com.gb.canibuythat.model.Spending
import com.gb.canibuythat.provider.BalanceCalculator
import com.gb.canibuythat.provider.Contract
import com.gb.canibuythat.provider.SpendingDbHelper
import com.j256.ormlite.dao.Dao
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import java.sql.SQLException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpendingsRepository @Inject
constructor(spendingDbHelper: SpendingDbHelper, private val userPreferences: UserPreferences) {
    private val spendingDao: Dao<Spending, Int> = spendingDbHelper.getDao<Dao<Spending, Int>, Spending>(Spending::class.java)

    val all: Single<List<Spending>>
        get() {
            return Single.create<List<Spending>> { emitter ->
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

    /**
     * Every spending has a local category and a monzo category.
     *
     * For each spending:
     *
     *     - if it does not yet exist in the databse, create it
     *     - if it does exist, update its name, value, cycle start and end date, enabled and spent
     *
     * @param spendings from Monzo
     */
    fun createOrUpdateMonzoSpendings(spendings: List<Spending>): Completable {
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
                            it.type = savedSpendings[index].type
                            it.occurrenceCount = savedSpendings[index].occurrenceCount
                            it.enabled = savedSpendings[index].enabled

                            if (savedSpendings[index].value != 0.0) {
                                it.value = savedSpendings[index].value
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

    fun deleteAll(): Completable {
        return Completable.create { emitter ->
            try {
                if (spendingDao.delete(spendingDao.deleteBuilder().prepare()) > 0) {
                    emitter.onComplete()
                } else {
                    emitter.onError(Exception("Delete error: couldn't clear table"))
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

    fun getBalance(): Single<Balance> {
        return Single.just(calculateBalance(null))
    }

    fun getCategoryBalance(): String {
        val buffer = StringBuffer()
        Spending.Category.values().forEach {
            val (best, worst) = calculateBalance(it.name)
            buffer.append("${it.name}: ${best}/${worst}\n")
        }
        return buffer.toString()
    }

    private fun calculateBalance(category: String?): Balance {
        val balance = Balance()

        // blocking thread
        val balanceReading = userPreferences.balanceReading

        val query: MutableMap<String, Any> = mutableMapOf(Contract.Spending.ENABLED to true)
        category?.let { query.put(Contract.Spending.TYPE, category) }

        for (spending: Spending in spendingDao.queryForFieldValues(query)) {
            val startDate = balanceReading?.`when`
            val result: BalanceCalculator.BalanceResult = BalanceCalculator.getEstimatedBalance(
                    spending, startDate, userPreferences.estimateDate)
            balance.bestCase += result.bestCase
            balance.worstCase += result.worstCase
        }

        balanceReading?.let {
            balance.bestCase += balanceReading.balance
            balance.worstCase += balanceReading.balance
        }
        return balance
    }
}
