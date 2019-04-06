package com.gb.canibuyit.feature.spending.data

import com.gb.canibuyit.UserPreferences
import com.gb.canibuyit.feature.spending.persistence.Contract
import com.gb.canibuyit.feature.spending.persistence.model.ApiSpending
import com.gb.canibuyit.error.DomainException
import com.gb.canibuyit.feature.spending.model.Spending
import com.gb.canibuyit.feature.spending.ui.BalanceBreakdown
import com.gb.canibuyit.util.formatDayMonth
import com.gb.canibuyit.util.formatDayMonthYear
import com.gb.canibuyit.util.fromJson
import com.google.gson.Gson
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.stmt.Where
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import java.math.BigDecimal
import java.sql.SQLException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpendingsRepository @Inject
constructor(private val dao: Dao<ApiSpending, Int>,
            private val mapper: SpendingMapper,
            private val prefs: UserPreferences,
            private val gson: Gson) {

    fun getAll(): Single<List<Spending>> {
        return Single.create<List<Spending>> { emitter ->
            try {
                emitter.onSuccess(dao.queryForAll().map(mapper::map))
            } catch (e: SQLException) {
                emitter.onError(e)
            }
        }
    }

    /**
     * The specified spending will have it's id updated after a successful insert
     */
    fun createOrUpdate(spending: Spending): Completable {
        return Completable.create { emitter ->
            try {
                val apiSpending = mapper.map(spending)
                dao.createOrUpdate(apiSpending)
                emitter.onComplete()
                spending.id = apiSpending.id
                saveSavings(spending, apiSpending)
                saveSpentByCycle(spending, apiSpending)
            } catch (e: SQLException) {
                emitter.onError(e)
            }
        }
    }

    fun createOrUpdateSpendings(remoteSpendings: List<Spending>, remoteCategoryKey: String): Completable {
        return Completable.create { emitter ->
            val savedSpendings = dao.queryForAll()
            val savedCategories: MutableList<String?> = savedSpendings
                    .mapNotNull {
                        mapper.mapSourceData(it.sourceData)?.get(remoteCategoryKey)
                    }
                    .sortedBy { it }
                    .toMutableList()
            try {
                remoteSpendings.forEach { spending ->
                    val remoteCategory = spending.sourceData!![remoteCategoryKey]

                    // find the saved spending that has the same remote category as the current remote spending
                    // Note: no two remote spending should have the same remote category, so there will only be one match
                    val exists = savedCategories.remove(remoteCategory)

                    val apiSpending = mapper.map(spending)
                    if (exists) {
                        dao.update(apiSpending)
                    } else {
                        dao.create(apiSpending)
                        spending.id = apiSpending.id
                    }
                    saveSavings(spending, apiSpending)
                    saveSpentByCycle(spending, apiSpending)
                }
                // Disable leftovers (spendings that are no longer received)
                // This can happen when the user retroactively re-categorizes some spendings,
                // causing one or more of the categories to disappear
                savedSpendings.filter { savedCategories.contains(it.type.toString()) && it.enabled!! }
                        .forEach {
                            it.enabled = false
                            it.value = BigDecimal.ZERO
                            dao.update(it)
                        }
                emitter.onComplete()
            } catch (e: SQLException) {
                emitter.onError(Exception("Error creating/updating remote spendings", e))
            }
        }
    }

    private fun saveSavings(spending: Spending, apiSpending: ApiSpending) {
        dao.assignEmptyForeignCollection(apiSpending, Contract.Spending.SAVINGS)
        apiSpending.savings?.let { foreignCollection ->
            spending.savings?.map { mapper.map(it, apiSpending) }?.asIterable()?.also {
                foreignCollection.clear()
                foreignCollection.addAll(it)
            }
        }
    }

    private fun saveSpentByCycle(spending: Spending, apiSpending: ApiSpending) {
        dao.assignEmptyForeignCollection(apiSpending, Contract.Spending.CYCLE_SPENT)
        apiSpending.spentByCycle?.let { foreignCollection ->
            spending.spentByCycle?.map { mapper.map(it, apiSpending) }?.asIterable()?.also {
                foreignCollection.clear()
                foreignCollection.addAll(it)
            }
        }
    }

    fun deleteSpendByCycleBySpendingId(spending: Spending) {
        dao.queryForId(spending.id).spentByCycle?.clear()
        spending.spentByCycle = emptyList()
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

    fun get(id: Int?): Maybe<Spending> {
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

    fun getSpendingByRemoteCategory(category: String, remoteCategoryKey: String): Observable<Spending> {
        return Observable.create<Spending> { emitter ->
            try {
                dao.queryForAll()
                        .map {
                            Pair<ApiSpending, Map<String, String>?>(
                                    it,
                                    it.sourceData?.let {
                                        gson.fromJson<Map<String, String>>(it)
                                    })
                        }
                        .filter {
                            it.second?.get(remoteCategoryKey)?.equals(category)
                                    ?: false
                        }
                        .forEach { emitter.onNext(mapper.map(it.first)) }
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
        return try {
            val balance = calculateBalanceForCategory(null, startDate = prefs.balanceReading?.date, endDate = prefs.estimateDate)
            prefs.balanceReading?.let { reading ->
                balance.amount += reading.balance
                balance.target += reading.balance
            }
            Single.just(balance)
        } catch (e: IllegalArgumentException) {
            Single.error(DomainException("Date of balance reading must not come after date of target estimate", e))
        } catch (e: Throwable) {
            Single.error(e)
        }
    }

    /**
     * Fetch breakdown of projection
     */
    fun getBalanceBreakdown(): BalanceBreakdown {
        var totalIncomeStr: String? = null
        var totalExpenseStr: String? = null
        val result = mutableListOf<Pair<ApiSpending.Category, String>>()
        prefs.balanceReading?.let { reading ->
            val startDate = reading.date
            val endDate = prefs.estimateDate
            val totalExpense = calculateTotalBalanceExceptForCategory(ApiSpending.Category.INCOME, startDate = startDate, endDate = endDate)
            try {
                ApiSpending.Category.values()
                        .map { Pair(it, calculateBalanceForCategory(it, startDate, endDate)) }
                        .filter { it.second.amount != 0f }
                        .sortedByDescending { Math.abs(it.second.amount) }
                        .forEach {
                            val category = it.first
                            val balance = it.second.amount
                            val name = category.name.substring(0, Math.min(10, category.name.length))
                                    .toLowerCase().capitalize()
                            val amount: String = "%1\$.0f".format(balance)

                            result.add(Pair(category, if (category != ApiSpending.Category.INCOME) {
                                val percent = balance / totalExpense.amount * 100
                                "%1\$s: %2\$s (%3\$.1f%%)".format(name, amount, percent)
                            } else {
                                "%1\$s: %2\$s".format(name, amount)
                            }))
                        }
                val totalIncome = calculateBalanceForCategory(ApiSpending.Category.INCOME, startDate = startDate, endDate = endDate)
                totalIncomeStr = "Tots. in: ${totalIncome.amount}"
                totalExpenseStr = "Tots. out: ${totalExpense.amount}"
            } catch (e: Throwable) {
                throw DomainException("Error calculating balance breakdown", e)
            }
        }

        return BalanceBreakdown(result.toTypedArray(), totalIncomeStr, totalExpenseStr)
    }

    /**
     * Fetch breakdown of target projection
     */
    fun getTargetBalanceBreakdown(): String {
        val buffer = StringBuffer()
        prefs.balanceReading?.let { balanceReading ->
            val startDate = balanceReading.date
            val endDate = prefs.estimateDate
            val total = calculateTotalBalanceExceptForCategory(ApiSpending.Category.INCOME, startDate = startDate, endDate = endDate).target
            try {
                ApiSpending.Category.values()
                        .map { Pair(it, calculateBalanceForCategory(it, startDate, endDate)) }
                        .filter { it.second.target != 0f }
                        .sortedBy { it.second.target }
                        .joinTo(buffer = buffer, separator = "\n", transform = {
                            val name = it.first.name.substring(0, Math.min(12, it.first.name.length))
                                    .toLowerCase().capitalize()
                            val amount = "%1\$.0f".format(it.second.target)

                            if (it.first != ApiSpending.Category.INCOME) {
                                val percent = it.second.target.div(total).times(100)
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
            val startDate = balanceReading.date
            val endDate = prefs.estimateDate
            var hasNegAmounts = false
            try {
                ApiSpending.Category.values()
                        .map { Pair(it, calculateBalanceForCategory(it, startDate, endDate)) }
                        .filter { (it.second.target - it.second.amount) != 0f }
                        .sortedByDescending { Math.abs(it.second.target) }
                        .joinTo(buffer = buffer, separator = "\n", transform = {
                            val name = it.first.name.substring(0, Math.min(12, it.first.name.length))
                                    .toLowerCase().capitalize()
                            val amount = it.second.target - it.second.amount
                            val amountStr = "%1\$.0f".format(amount)
                            if (amount > 0) {
                                "%1\$s: %2\$s".format(name, amountStr)
                            } else {
                                hasNegAmounts = true
                                "%1\$s: %2\$s*".format(name, amountStr)
                            }
                        })
            } catch (e: IllegalArgumentException) {
                throw DomainException("Date of balance reading must not come after date of target estimate", e)
            }
            val balance = calculateTotalBalanceExceptForCategory(ApiSpending.Category.INCOME, startDate = startDate, endDate = endDate)
            val amount = balance.target - balance.amount
            buffer.append("\n-----------------\nTotal: ")
            buffer.append("%1\$.0f".format(amount))
            if (hasNegAmounts) {
                buffer.append("\n\n*Negative value means your average spending is now less then your limit in that category. Time to lower the limit?")
            }
        }
        return buffer.toString()
    }

    fun getBalanceBreakdownCategoryDetails(category: ApiSpending.Category): String? {
        return prefs.balanceReading?.let { reading ->
            val startDate = reading.date
            val endDate = prefs.estimateDate
            val balance = calculateBalanceForCategory(category, startDate, endDate)
            val buffer = StringBuffer()
            var index = 0
            balance.spendingEvents?.joinTo(buffer = buffer, separator = "\n", transform = {
                index++
                if (endDate > it.end) {
                    "$index. ${it.start.formatDayMonth()} - ${it.end.formatDayMonthYear()} (${it.amount})"
                } else {
                    "$index. ${it.start.formatDayMonth()}( - ${it.end.formatDayMonthYear()}) (${it.amount})"
                }
            }).toString()
        }
    }

    /**
     * @param category for which the balance should be calculated. If null, all categories will be included
     * @param startDate from which the calculation should start. If null, the individual spending start-dates will be used
     * @param endDate up until which the calculations should go. If null, `today` is used
     */
    private fun calculateBalanceForCategory(category: ApiSpending.Category?, startDate: LocalDate?, endDate: LocalDate): Balance {
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
    private fun calculateTotalBalanceExceptForCategory(omittedCategory: ApiSpending.Category, startDate: LocalDate?, endDate: LocalDate): Balance {
        val builder: Where<ApiSpending, Int> = dao.queryBuilder().where()
                .notIn(Contract.Spending.TYPE, omittedCategory)
        return calculateBalance(builder, startDate, endDate)
    }

    private fun calculateBalance(builder: Where<ApiSpending, Int>, startDate: LocalDate?, endDate: LocalDate): Balance {
        val spendingEvents = mutableListOf<SpendingEvent>()
        val balance = Balance(0f, 0f, null)
        dao.query(builder.prepare()).forEach { spending ->
            val (amount, target, spendingEventsOut)
                    = BalanceCalculator.getEstimatedBalance(mapper.map(spending), startDate, endDate)
            balance.amount += amount
            balance.target += target
            spendingEventsOut?.forEach {
                spendingEvents.add(it)
            }
        }
        spendingEvents.sortBy { it.start }
        balance.spendingEvents = spendingEvents.toTypedArray()
        return balance
    }
}
