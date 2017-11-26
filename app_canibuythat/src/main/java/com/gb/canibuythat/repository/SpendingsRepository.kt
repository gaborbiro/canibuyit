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
constructor(private val spendingDao: Dao<Spending, Int>,
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
        return try {
            val balance = calculateBalanceForCategory(null, startDate = userPreferences.balanceReading?.`when`, endDate = userPreferences.estimateDate)
            userPreferences.balanceReading?.let {
                balance.definitely = balance.definitely.plus(it.balance)
                balance.maybeEvenThisMuch = balance.maybeEvenThisMuch.plus(it.balance)
                balance.targetDefinitely = balance.targetDefinitely.plus(it.balance)
                balance.targetMaybeEvenThisMuch = balance.targetMaybeEvenThisMuch.plus(it.balance)
            }
            Single.just(balance)
        } catch (e: IllegalArgumentException) {
            Single.error(DomainException("Date of balance reading must not come after date of target estimate", e))
        }
    }

    fun getBalanceBreakdown(): String {
        val buffer = StringBuffer()
        userPreferences.balanceReading?.let { balanceReading ->
            val startDate = balanceReading.`when`
            val endDate = userPreferences.estimateDate
            val total = calculateBalanceButForCategory(Spending.Category.INCOME, startDate = startDate, endDate = endDate)!!.definitely
            try {
                Spending.Category.values()
                        .map { Pair(it, calculateBalanceForCategory(it, startDate, endDate)) }
                        .filter { it.second.definitely != 0f || it.second.maybeEvenThisMuch != 0f }
                        .sortedBy { it.second.definitely }
                        .joinTo(buffer = buffer, separator = "\n", transform = {
                            val name = it.first.name.substring(0, Math.min(12, it.first.name.length)).toLowerCase().capitalize()
                            val definitely = it.second.definitely
                            val maybe = it.second.maybeEvenThisMuch
                            val amount = if (definitely == maybe) "%1\$.0f".format(definitely) else "%1\$.0f/%2\$.0f".format(definitely, maybe - definitely)

                            if (it.first != Spending.Category.INCOME) {
                                val percent = definitely.div(total).times(100)
                                "%1\$s: %2\$s (%3\$.1f%%)".format(name, amount, percent)
                            } else {
                                "%1\$s: %2\$s".format(name, amount)
                            }
                        })
            } catch (e: IllegalArgumentException) {
                throw DomainException("Date of balance reading must not come after date of target estimate", e)
            }
        }
        return buffer.toString()
    }

    fun getTargetBalanceBreakdown(): String {
        val buffer = StringBuffer()
        userPreferences.balanceReading?.let { balanceReading ->
            val startDate = balanceReading.`when`
            val endDate = userPreferences.estimateDate
            val total = calculateBalanceButForCategory(Spending.Category.INCOME, startDate = startDate, endDate = endDate)!!.targetDefinitely
            try {
                Spending.Category.values()
                        .map { Pair(it, calculateBalanceForCategory(it, startDate, endDate)) }
                        .filter { it.second.targetDefinitely != 0f || it.second.targetMaybeEvenThisMuch != 0f }
                        .sortedBy { it.second.targetDefinitely }
                        .joinTo(buffer = buffer, separator = "\n", transform = {
                            val name = it.first.name.substring(0, Math.min(12, it.first.name.length)).toLowerCase().capitalize()
                            val definitely = it.second.targetDefinitely
                            val maybe = it.second.targetMaybeEvenThisMuch
                            val amount = if (definitely == maybe) "%1\$.0f".format(definitely) else "%1\$.0f/%2\$.0f".format(definitely, maybe - definitely)

                            if (it.first != Spending.Category.INCOME) {
                                val percent = definitely.div(total).times(100)
                                "%1\$s: %2\$s (%3\$.1f%%)".format(name, amount, percent)
                            } else {
                                "%1\$s: %2\$s".format(name, amount)
                            }
                        })
            } catch (e: IllegalArgumentException) {
                throw DomainException("Date of balance reading must not come after date of target estimate", e)
            }
        }
        return buffer.toString()
    }

    fun getTargetSavingBreakdown(): String {
        val buffer = StringBuffer()
        userPreferences.balanceReading?.let { balanceReading ->
            val startDate = balanceReading.`when`
            val endDate = userPreferences.estimateDate
            try {
                Spending.Category.values()
                        .map { Pair(it, calculateBalanceForCategory(it, startDate, endDate)) }
                        .filter { (it.second.targetDefinitely - it.second.definitely) != 0f || (it.second.targetMaybeEvenThisMuch - it.second.maybeEvenThisMuch) != 0f }
                        .sortedBy { it.second.targetDefinitely }
                        .joinTo(buffer = buffer, separator = "\n", transform = {
                            val name = it.first.name.substring(0, Math.min(12, it.first.name.length)).toLowerCase().capitalize()
                            val definitely = it.second.targetDefinitely - it.second.definitely
                            val maybe = it.second.targetMaybeEvenThisMuch - it.second.maybeEvenThisMuch
                            val amount = if (definitely == maybe) "%1\$.0f".format(definitely) else "%1\$.0f/%2\$.0f".format(definitely, maybe - definitely)
                            "%1\$s: %2\$s".format(name, amount)
                        })
            } catch (e: IllegalArgumentException) {
                throw DomainException("Date of balance reading must not come after date of target estimate", e)
            }
            val balance = calculateBalanceButForCategory(Spending.Category.INCOME, startDate = startDate, endDate = endDate)!!
            val definitely = balance.targetDefinitely - balance.definitely
            val maybe = balance.targetMaybeEvenThisMuch - balance.maybeEvenThisMuch
            buffer.append("\n-----------------\nTotal: ")
            buffer.append("%1\$.0f/%2\$.0f".format(definitely, maybe))
        }
        return buffer.toString()
    }

    /**
     * @param category for which the balance should be calculated. If null, all categories will be included.
     * @param startDate from which the calculation should start. If null, the individual spending start-dates will be used.
     * @param endDate up until which the calculations should go. If null, `today` is used.
     */
    private fun calculateBalanceForCategory(category: Spending.Category?, startDate: Date?, endDate: Date): Balance {
        val balance = Balance(0f, 0f, 0f, 0f)

        val query: MutableMap<String, Any> = mutableMapOf(Contract.Spending.ENABLED to true)
        category?.let { query.put(Contract.Spending.TYPE, category) }
        // blocking thread


        for (spending: Spending in spendingDao.queryForFieldValues(query)) {
            val (definitely, maybeEvenThisMuch, targetDefinitely, targetMaybeEvenThisMuch, _) = BalanceCalculator.getEstimatedBalance(spending, startDate, endDate)
            balance.definitely = balance.definitely.plus(definitely)
            balance.maybeEvenThisMuch = balance.maybeEvenThisMuch.plus(maybeEvenThisMuch)
            balance.targetDefinitely = balance.targetDefinitely.plus(targetDefinitely)
            balance.targetMaybeEvenThisMuch = balance.targetMaybeEvenThisMuch.plus(targetMaybeEvenThisMuch)
        }

        return balance
    }

    /**
     * @param category which should be omitted from the total.
     * @param startDate from which the calculation should start. If null, the individual spending start-dates will be used.
     * @param endDate up until which the calculations should go. If null, `today` is used.
     */
    private fun calculateBalanceButForCategory(category: Spending.Category, startDate: Date?, endDate: Date): Balance? {
        var balance: Balance? = null
        val builder = spendingDao.queryBuilder()
        builder.where().notIn(Contract.Spending.TYPE, category)
        for (spending: Spending in spendingDao.query(builder.prepare())) {
            if (balance == null) {
                balance = Balance(0f, 0f, 0f, 0f)
            }
            val (definitely, maybeEvenThisMuch, targetDefinitely, targetMaybeEvenThisMuch, _) = BalanceCalculator.getEstimatedBalance(
                    spending, startDate, endDate)
            balance.definitely = balance.definitely.plus(definitely)
            balance.maybeEvenThisMuch = balance.maybeEvenThisMuch.plus(maybeEvenThisMuch)
            balance.targetDefinitely = balance.targetDefinitely.plus(targetDefinitely)
            balance.targetMaybeEvenThisMuch = balance.targetMaybeEvenThisMuch.plus(targetMaybeEvenThisMuch)
        }

        return balance
    }
}
