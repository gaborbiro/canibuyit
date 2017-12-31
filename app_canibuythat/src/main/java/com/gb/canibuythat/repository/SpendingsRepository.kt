package com.gb.canibuythat.repository

import com.gb.canibuythat.UserPreferences
import com.gb.canibuythat.db.Contract
import com.gb.canibuythat.db.model.ApiSpending
import com.gb.canibuythat.exception.DomainException
import com.gb.canibuythat.model.Balance
import com.gb.canibuythat.model.Spending
import com.gb.canibuythat.model.SpendingEvent
import com.gb.canibuythat.util.DateUtils
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.stmt.Where
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
constructor(private val dao: Dao<ApiSpending, Int>,
            private val mapper: SpendingMapper,
            private val prefs: UserPreferences) {

    val all: Single<List<Spending>>
        get() {
            return Single.create<List<Spending>> { emitter ->
                try {
                    emitter.onSuccess(dao.queryForAll().map(mapper::map))
                } catch (e: SQLException) {
                    emitter.onError(e)
                }
            }
        }

    fun createOrUpdate(spending: Spending): Single<Dao.CreateOrUpdateStatus> {
        return Single.create { emitter ->
            try {
                emitter.onSuccess(dao.createOrUpdate(mapper.map(spending)))
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
            val savedMonzoSpendings: List<ApiSpending> = dao.queryForAll()
                    .filter { it.sourceData?.containsKey(ApiSpending.SOURCE_MONZO_CATEGORY) == true }
                    .sortedBy { it.sourceData?.get(ApiSpending.SOURCE_MONZO_CATEGORY) }

            try {
                remoteSpendings.forEach {
                    val remoteMonzoCategory = it.sourceData!![ApiSpending.SOURCE_MONZO_CATEGORY]

                    // find the saved spending that has the same monzo category as the current remote spending
                    // Note: at the moment no two monzo spending will have the same monzo category, so there will only be one match
                    val index = savedMonzoSpendings.indexOfFirst { it.sourceData?.get(ApiSpending.SOURCE_MONZO_CATEGORY) == remoteMonzoCategory }

                    if (index >= 0) {
                        dao.update(mapper.map(it))
                    } else {
                        dao.create(mapper.map(it))
                    }
                }
                emitter.onComplete()
            } catch (e: SQLException) {
                emitter.onError(Exception("Error creating/updating monzo spendings", e))
            }
        }
    }

    fun delete(id: Int): Completable {
        return Completable.create { emitter ->
            try {
                if (dao.deleteById(id) > 0) {
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
                if (dao.delete(dao.deleteBuilder().prepare()) > 0) {
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
                val spending = dao.queryForId(id)

                if (spending != null) {
                    emitter.onSuccess(mapper.map(spending))
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
                dao.queryForAll()
                        .filter { it.sourceData?.get(ApiSpending.SOURCE_MONZO_CATEGORY)?.equals(category) ?: false }
                        .forEach { emitter.onNext(mapper.map(it)) }
                emitter.onComplete()
            } catch (e: SQLException) {
                emitter.onError(e)
            }
        }
    }

    /**
     * Fetch all enabled spending items that have a target set and also have `spent` value set
     */
    fun getSpendingsWithTarget(): Single<Array<Spending>> {
        return Single.create<Array<Spending>> { emitter ->
            try {
                val builder = dao.queryBuilder().where()
                builder.isNotNull(Contract.Spending.TARGET)
                builder.and()
                builder.isNotNull(Contract.Spending.SPENT)
                builder.and()
                builder.eq(Contract.Spending.ENABLED, true)
                emitter.onSuccess(dao.query(builder.prepare()).toList().map(mapper::map).toTypedArray())
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
            val balance = calculateBalanceForCategory(null, startDate = prefs.balanceReading?.`when`, endDate = prefs.estimateDate)
            prefs.balanceReading?.let { reading ->
                balance.definitely += reading.balance
                balance.maybeEvenThisMuch += reading.balance
                balance.targetDefinitely += reading.balance
                balance.targetMaybeEvenThisMuch += reading.balance
            }
            return Single.just(balance)
        } catch (e: IllegalArgumentException) {
            return Single.error(DomainException("Date of balance reading must not come after date of target estimate", e))
        } catch (e: Throwable) {
            return Single.error(e)
        }
    }

    /**
     * Fetch breakdown of projection
     */
    fun getBalanceBreakdown(): Array<Pair<ApiSpending.Category, String>> {
        val result = mutableListOf<Pair<ApiSpending.Category, String>>()
        prefs.balanceReading?.let { reading ->
            val startDate = reading.`when`
            val endDate = prefs.estimateDate
            val total = calculateTotalBalanceExceptForCategory(ApiSpending.Category.INCOME, startDate = startDate, endDate = endDate).definitely
            try {
                ApiSpending.Category.values()
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

                            result.add(Pair(category, if (category != ApiSpending.Category.INCOME) {
                                val percent = definitely / total * 100
                                "%1\$s: %2\$s (%3\$.1f%%)".format(name, amount, percent)
                            } else {
                                "%1\$s: %2\$s".format(name, amount)
                            }))
                        }
            } catch (e: Throwable) {
                throw DomainException("Error calculating balance breakdown", e)
            }
        }
        return result.toTypedArray()
    }

    /**
     * Fetch breakdown of target projection
     */
    fun getTargetBalanceBreakdown(): String {
        val buffer = StringBuffer()
        prefs.balanceReading?.let { balanceReading ->
            val startDate = balanceReading.`when`
            val endDate = prefs.estimateDate
            val total = calculateTotalBalanceExceptForCategory(ApiSpending.Category.INCOME, startDate = startDate, endDate = endDate).targetDefinitely
            try {
                ApiSpending.Category.values()
                        .map { Pair(it, calculateBalanceForCategory(it, startDate, endDate)) }
                        .filter { it.second.targetDefinitely != 0f || it.second.targetMaybeEvenThisMuch != 0f }
                        .sortedBy { it.second.targetDefinitely }
                        .joinTo(buffer = buffer, separator = "\n", transform = {
                            val name = it.first.name.substring(0, Math.min(12, it.first.name.length)).toLowerCase().capitalize()
                            val definitely = it.second.targetDefinitely
                            val maybe = it.second.targetMaybeEvenThisMuch
                            val amount = if (definitely == maybe) "%1\$.0f".format(definitely) else "%1\$.0f/%2\$.0f".format(definitely, maybe)

                            if (it.first != ApiSpending.Category.INCOME) {
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
        prefs.balanceReading?.let { balanceReading ->
            val startDate = balanceReading.`when`
            val endDate = prefs.estimateDate
            var hasNegAmounts = false
            try {
                ApiSpending.Category.values()
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
            val balance = calculateTotalBalanceExceptForCategory(ApiSpending.Category.INCOME, startDate = startDate, endDate = endDate)
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

    fun getBalanceBreakdownCategoryDetails(category: ApiSpending.Category): String? {
        return prefs.balanceReading?.let { reading ->
            val startDate = reading.`when`
            val endDate = prefs.estimateDate
            val balance = calculateBalanceForCategory(category, startDate, endDate)
            val buffer = StringBuffer()
            var index = 0
            balance.spendingEvents?.joinTo(buffer = buffer, separator = "\n", transform = {
                index++
                val amount = if (it.definitely == it.maybe) "${it.definitely}" else "${it.definitely}/${it.maybe}"
                if (endDate.after(it.end)) {
                    "$index. ${DateUtils.formatDayMonth(it.start)} - ${DateUtils.formatDayMonthYear(it.end)} ($amount)"
                } else {
                    "$index. ${DateUtils.formatDayMonth(it.start)}( - ${DateUtils.formatDayMonthYear(it.end)}) ($amount)"
                }
            }).toString()
        }
    }

    /**
     * @param category for which the balance should be calculated. If null, all categories will be included
     * @param startDate from which the calculation should start. If null, the individual spending start-dates will be used
     * @param endDate up until which the calculations should go. If null, `today` is used
     */
    private fun calculateBalanceForCategory(category: ApiSpending.Category?, startDate: Date?, endDate: Date): Balance {
        val builder = dao.queryBuilder().where()
        category?.let { builder.`in`(Contract.Spending.TYPE, it).and() }
        builder.eq(Contract.Spending.ENABLED, true)
        return calculateBalance(builder, startDate, endDate)
    }

    /**
     * @param omittedCategory category that should be omitted from the total
     * @param startDate from which the calculation should start. If null, the start-dates of the spendings will be used
     * @param endDate up until which the calculations should go. If null, `today` is used
     */
    private fun calculateTotalBalanceExceptForCategory(omittedCategory: ApiSpending.Category, startDate: Date?, endDate: Date): Balance {
        val builder: Where<ApiSpending, Int> = dao.queryBuilder().where()
                .notIn(Contract.Spending.TYPE, omittedCategory)
        return calculateBalance(builder, startDate, endDate)
    }

    private fun calculateBalance(builder: Where<ApiSpending, Int>, startDate: Date?, endDate: Date): Balance {
        val spendingEvents = mutableListOf<SpendingEvent>()
        val balance = Balance(0f, 0f, 0f, 0f, null)
        dao.query(builder.prepare()).forEach { spending ->
            val (definitely, maybeEvenThisMuch, targetDefinitely, targetMaybeEvenThisMuch, spendingEventsOut)
                    = BalanceCalculator.getEstimatedBalance(mapper.map(spending), startDate, endDate)
            balance.definitely += definitely
            balance.maybeEvenThisMuch += maybeEvenThisMuch
            balance.targetDefinitely += targetDefinitely
            balance.targetMaybeEvenThisMuch += targetMaybeEvenThisMuch
            spendingEventsOut?.forEach {
                spendingEvents.add(it)
            }
        }
        spendingEvents.sortBy { it.start }
        balance.spendingEvents = spendingEvents.toTypedArray()
        return balance
    }
}
