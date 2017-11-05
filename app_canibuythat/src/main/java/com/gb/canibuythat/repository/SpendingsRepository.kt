package com.gb.canibuythat.repository

import com.gb.canibuythat.UserPreferences
import com.gb.canibuythat.db.Contract
import com.gb.canibuythat.exception.DomainException
import com.gb.canibuythat.model.Balance
import com.gb.canibuythat.model.Spending
import com.j256.ormlite.dao.Dao
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import java.sql.SQLException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpendingsRepository @Inject
constructor(val spendingDao: Dao<Spending, Int>,
            private val userPreferences: UserPreferences) {

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
     *     - if it does exist, update its fields selectively
     *
     * @param spendings from Monzo
     */
    fun createOrUpdateMonzoSpendings(spendings: List<Spending>): Completable {
        return Completable.create { emitter ->
            val savedMonzoSpendings: List<Spending> = spendingDao.queryForAll()
                    .filter { it.sourceData[Spending.SOURCE_MONZO_CATEGORY] != null }
                    .sortedBy { it.sourceData[Spending.SOURCE_MONZO_CATEGORY] }

            try {
                spendings.forEach {
                    val category: String? = it.sourceData[Spending.SOURCE_MONZO_CATEGORY]

                    if (category == null) {
                        emitter.onError(Exception("Monzo spending '" + it.name + "' has no category"))
                    } else {
                        val index = savedMonzoSpendings.indexOfFirst { it.sourceData[Spending.SOURCE_MONZO_CATEGORY] == category }

                        if (index >= 0) {
                            it.id = savedMonzoSpendings[index].id
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

    fun get(id: Int): Maybe<Spending> {
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

    fun getByMonzoCategory(category: String): Observable<Spending> {
        return Observable.create<Spending> { emitter ->
            try {
                val spendings: List<Spending> = spendingDao.queryForAll()
                        .filter { it.sourceData[Spending.SOURCE_MONZO_CATEGORY]?.equals(category) ?: false }
                spendings.forEach { emitter.onNext(it) }
                emitter.onComplete()
            } catch (e: SQLException) {
                emitter.onError(e)
            }
        }
    }

    fun getBalance(): Single<Balance> {
        try {
            val balance = calculateBalanceForCategory(null, startDate = userPreferences.balanceReading?.`when`, endDate = userPreferences.estimateDate)
            balance?.let {
                userPreferences.balanceReading?.let {
                    balance.definitely = balance.definitely?.plus(it.balance)
                    balance.maybeEvenThisMuch = balance.maybeEvenThisMuch?.plus(it.balance)
                }
                return Single.just(balance)
            }
            return Single.just(Balance())
        } catch (e: IllegalArgumentException) {
            return Single.error(DomainException("Date of balance reading must not come after date of target estimate", e))
        }
    }

    fun getCategoryBalance(): String {
        val buffer = StringBuffer()
        userPreferences.balanceReading?.let { balanceReading ->
            val startDate = balanceReading.`when`
            val endDate = userPreferences.estimateDate
            val total = calculateBalanceForCategory(null, startDate = startDate, endDate = endDate)!!.definitely!!
            try {
                Spending.Category.values()
                        .map { Pair(it, calculateBalanceForCategory(it, startDate, endDate)) }
                        .filter { it.second?.let { it.definitely != 0f || it.maybeEvenThisMuch != 0f } ?: false }
                        .sortedBy { it.second!!.definitely }
                        .joinTo(buffer = buffer, separator = "\n", transform = {
                            val name = it.first.name.toLowerCase().capitalize()
                            val definitely = it.second!!.definitely!!
                            val maybe = it.second!!.maybeEvenThisMuch
                            val percent = definitely.div(total).times(100)
                            "%1\$s: %2\$.0f/%3\$.0f (%4\$.0f%%)".format(name, definitely, maybe, percent)
                        })
            } catch (e: IllegalArgumentException) {
                throw DomainException("Date of balance reading must not come after date of target estimate", e)
            }
        }
        return buffer.toString()
    }

    /**
     * @param category for which the balance should be calculated. If null, all categories will be included.
     * @param startDate from which the calculation should start. If null, the individual spending start-dates will be used.
     * @param endDate up until which the calculations should go. If null, `today` is used.
     */
    private fun calculateBalanceForCategory(category: Spending.Category?, startDate: Date?, endDate: Date): Balance? {
        var balance: Balance? = null

        val query: MutableMap<String, Any> = mutableMapOf(Contract.Spending.ENABLED to true)
        category?.let { query.put(Contract.Spending.TYPE, category) }
        // blocking thread


        for (spending: Spending in spendingDao.queryForFieldValues(query)) {
            if (balance == null) {
                balance = Balance(0f, 0f)
            }
            val (definitely, maybeEvenThisMuch, _) = BalanceCalculator.getEstimatedBalance(
                    spending, startDate, endDate)
            balance.definitely = balance.definitely?.plus(definitely)
            balance.maybeEvenThisMuch = balance.maybeEvenThisMuch?.plus(maybeEvenThisMuch)
        }

        return balance
    }
}
