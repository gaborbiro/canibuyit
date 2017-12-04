package com.gb.canibuythat.repository

import com.gb.canibuythat.UserPreferences
import com.gb.canibuythat.db.Contract
import com.gb.canibuythat.exception.DomainException
import com.gb.canibuythat.model.Balance
import com.gb.canibuythat.model.Spending
import com.gb.canibuythat.util.DateUtils
import com.j256.ormlite.dao.Dao
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import java.sql.SQLException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.HashMap

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
     * This method used the monzo category to identify the spendings
     *
     * For each spending:
     *     - if it does not yet exist in the database, create it
     *     - if it does exist, update its fields selectively
     *
     * @param remoteSpendings from Monzo
     */
    fun createOrUpdateMonzoSpendings(remoteSpendings: List<Spending>): Completable {
        return Completable.create { emitter ->
            val savedMonzoSpendings: List<Spending> = spendingDao.queryForAll()
                    .filter { it.sourceData.containsKey(Spending.SOURCE_MONZO_CATEGORY) }
                    .sortedBy { it.sourceData[Spending.SOURCE_MONZO_CATEGORY] }

            try {
                remoteSpendings.forEach {
                    val remoteMonzoCategory = it.sourceData[Spending.SOURCE_MONZO_CATEGORY]

                    // find the saved spending that has the same monzo category as the current remote spending
                    // Note: at the moment no two monzo spending will have the same monzo category, so there will only be one match
                    val index = savedMonzoSpendings.indexOfFirst { it.sourceData[Spending.SOURCE_MONZO_CATEGORY] == remoteMonzoCategory }

                    if (index >= 0) {
                        it.id = savedMonzoSpendings[index].id
                        spendingDao.update(it)
                    } else {
                        spendingDao.create(it)
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

    fun getSpendingByMonzoCategory(category: String): Observable<Spending> {
        return Observable.create<Spending> { emitter ->
            try {
                spendingDao.queryForAll()
                        .filter { it.sourceData[Spending.SOURCE_MONZO_CATEGORY]?.equals(category) ?: false }
                        .forEach { emitter.onNext(it) }
                emitter.onComplete()
            } catch (e: SQLException) {
                emitter.onError(e)
            }
        }
    }

    /**
     * Fetch projection and target projection
     */
    fun getBalance(): Single<Balance> {
        try {
            val balance = calculateBalanceForCategory(null, startDate = userPreferences.balanceReading?.`when`, endDate = userPreferences.estimateDate)
            userPreferences.balanceReading?.let { reading ->
                balance.definitely += reading.balance
                balance.maybeEvenThisMuch += reading.balance
                balance.targetDefinitely += reading.balance
                balance.targetMaybeEvenThisMuch += reading.balance
            }
            return Single.just(balance)
        } catch (e: IllegalArgumentException) {
            return Single.error(DomainException("Date of balance reading must not come after date of target estimate", e))
        }
    }

    /**
     * Fetch breakdown of projection
     */
    fun getBalanceBreakdown(): HashMap<Spending.Category, String> {
        val result = HashMap<Spending.Category, String>()
        userPreferences.balanceReading?.let { reading ->
            val startDate = reading.`when`
            val endDate = userPreferences.estimateDate
            val total = calculateTotalBalanceExceptForCategory(Spending.Category.INCOME, startDate = startDate, endDate = endDate).definitely
            try {
                Spending.Category.values()
                        .map { Pair(it, calculateBalanceForCategory(it, startDate, endDate)) }
                        .filter { it.second.definitely != 0f || it.second.maybeEvenThisMuch != 0f }
                        .sortedBy { it.second.definitely }
                        .forEach {
                            val category = it.first
                            val balance = it.second
                            val name = category.name.substring(0, Math.min(12, category.name.length)).toLowerCase().capitalize()
                            val definitely = balance.definitely
                            val maybe = balance.maybeEvenThisMuch
                            val amount = if (definitely == maybe) "%1\$.0f".format(definitely) else "%1\$.0f/%2\$.0f".format(definitely, maybe)

                            result[category] = if (category != Spending.Category.INCOME) {
                                val percent = definitely / total * 100
                                "%1\$s: %2\$s (%3\$.1f%%)".format(name, amount, percent)
                            } else {
                                "%1\$s: %2\$s".format(name, amount)
                            }
                        }
            } catch (e: IllegalArgumentException) {
                throw DomainException("Date of balance reading must not come after date of target estimate", e)
            }
        }
        return result
    }

    /**
     * Fetch breakdown of target projection
     */
    fun getTargetBalanceBreakdown(): String {
        val buffer = StringBuffer()
        userPreferences.balanceReading?.let { balanceReading ->
            val startDate = balanceReading.`when`
            val endDate = userPreferences.estimateDate
            val total = calculateTotalBalanceExceptForCategory(Spending.Category.INCOME, startDate = startDate, endDate = endDate).targetDefinitely
            try {
                Spending.Category.values()
                        .map { Pair(it, calculateBalanceForCategory(it, startDate, endDate)) }
                        .filter { it.second.targetDefinitely != 0f || it.second.targetMaybeEvenThisMuch != 0f }
                        .sortedBy { it.second.targetDefinitely }
                        .joinTo(buffer = buffer, separator = "\n", transform = {
                            val name = it.first.name.substring(0, Math.min(12, it.first.name.length)).toLowerCase().capitalize()
                            val definitely = it.second.targetDefinitely
                            val maybe = it.second.targetMaybeEvenThisMuch
                            val amount = if (definitely == maybe) "%1\$.0f".format(definitely) else "%1\$.0f/%2\$.0f".format(definitely, maybe)

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

    /**
     * Fetch the breakdown of the difference between projection and target projection
     */
    fun getSavingsBreakdown(): String {
        val buffer = StringBuffer()
        userPreferences.balanceReading?.let { balanceReading ->
            val startDate = balanceReading.`when`
            val endDate = userPreferences.estimateDate
            var hasNegAmounts = false
            try {
                Spending.Category.values()
                        .map { Pair(it, calculateBalanceForCategory(it, startDate, endDate)) }
                        .filter { (it.second.targetDefinitely - it.second.definitely) != 0f || (it.second.targetMaybeEvenThisMuch - it.second.maybeEvenThisMuch) != 0f }
                        .sortedBy { it.second.targetDefinitely }
                        .joinTo(buffer = buffer, separator = "\n", transform = {
                            val name = it.first.name.substring(0, Math.min(12, it.first.name.length)).toLowerCase().capitalize()
                            val definitely = it.second.targetDefinitely - it.second.definitely
                            val maybe = it.second.targetMaybeEvenThisMuch - it.second.maybeEvenThisMuch
                            val amount = if (definitely == maybe) "%1\$.0f".format(definitely) else "%1\$.0f/%2\$.0f".format(definitely, maybe)
                            if (definitely > 0 && maybe > 0) {
                                "%1\$s: %2\$s".format(name, amount)
                            } else {
                                hasNegAmounts = true
                                "%1\$s: %2\$s*".format(name, amount)
                            }
                        })
            } catch (e: IllegalArgumentException) {
                throw DomainException("Date of balance reading must not come after date of target estimate", e)
            }
            val balance = calculateTotalBalanceExceptForCategory(Spending.Category.INCOME, startDate = startDate, endDate = endDate)
            val definitely = balance.targetDefinitely - balance.definitely
            val maybe = balance.targetMaybeEvenThisMuch - balance.maybeEvenThisMuch
            buffer.append("\n-----------------\nTotal: ")
            buffer.append("%1\$.0f/%2\$.0f".format(definitely, maybe))
            if (hasNegAmounts) {
                buffer.append("\n\n*Negative value means the average has fallen below the target. Time to adjust the target?")
            }
        }
        return buffer.toString()
    }

    fun getBalanceBreakdownCategoryDetails(category: Spending.Category): String? {
        return userPreferences.balanceReading?.let { reading ->
            val startDate = reading.`when`
            val endDate = userPreferences.estimateDate
            val balance = calculateBalanceForCategory(category, startDate, endDate)
            val buffer = StringBuffer()
            balance.spendingEvents?.joinTo(buffer = buffer, separator = "\n", transform = {
                "${DateUtils.formatDayMonth(it[0])} - ${DateUtils.formatDayMonthYear(it[1])}"
            }).toString()
        }
    }

    /**
     * @param category for which the balance should be calculated. If null, all categories will be included
     * @param startDate from which the calculation should start. If null, the individual spending start-dates will be used
     * @param endDate up until which the calculations should go. If null, `today` is used
     */
    private fun calculateBalanceForCategory(category: Spending.Category?, startDate: Date?, endDate: Date): Balance {
        val balance = Balance(0f, 0f, 0f, 0f, null)

        val query: MutableMap<String, Any> = mutableMapOf(Contract.Spending.ENABLED to true)
        category?.let { query.put(Contract.Spending.TYPE, category) }

        spendingDao.queryForFieldValues(query).forEach { spending ->
            val (definitely, maybeEvenThisMuch, targetDefinitely, targetMaybeEvenThisMuch, spendingEvents)
                    = BalanceCalculator.getEstimatedBalance(spending, startDate, endDate)
            balance.definitely = balance.definitely.plus(definitely)
            balance.maybeEvenThisMuch = balance.maybeEvenThisMuch.plus(maybeEvenThisMuch)
            balance.targetDefinitely = balance.targetDefinitely.plus(targetDefinitely)
            balance.targetMaybeEvenThisMuch = balance.targetMaybeEvenThisMuch.plus(targetMaybeEvenThisMuch)
            balance.spendingEvents = spendingEvents
        }

        return balance
    }

    /**
     * @param omittedCategory category that should be omitted from the total
     * @param startDate from which the calculation should start. If null, the start-dates of the spendings will be used
     * @param endDate up until which the calculations should go. If null, `today` is used
     */
    private fun calculateTotalBalanceExceptForCategory(omittedCategory: Spending.Category, startDate: Date?, endDate: Date): Balance {
        val balance = Balance(0f, 0f, 0f, 0f, null)
        val builder = spendingDao.queryBuilder()
        builder.where().notIn(Contract.Spending.TYPE, omittedCategory)
        spendingDao.query(builder.prepare()).forEach { spending ->
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
