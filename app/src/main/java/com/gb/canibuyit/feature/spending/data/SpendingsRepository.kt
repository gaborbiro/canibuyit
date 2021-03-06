package com.gb.canibuyit.feature.spending.data

import com.gb.canibuyit.UserPreferences
import com.gb.canibuyit.error.DomainException
import com.gb.canibuyit.feature.spending.model.Spending
import com.gb.canibuyit.feature.spending.persistence.Contract
import com.gb.canibuyit.feature.spending.persistence.model.DBSpending
import com.gb.canibuyit.feature.spending.ui.BalanceBreakdown
import com.gb.canibuyit.util.formatDayMonth
import com.gb.canibuyit.util.formatDayMonthYear
import com.gb.canibuyit.util.fromJson
import com.gb.canibuyit.util.reverseSign
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
import kotlin.math.min

@Singleton
class SpendingsRepository @Inject
constructor(private val dao: Dao<DBSpending, Int>,
            private val mapper: SpendingMapper,
            private val prefs: UserPreferences,
            private val gson: Gson) {

    fun getAll(): Single<List<Spending>> {
        return Single.create { emitter ->
            try {
                emitter.onSuccess(dao.queryForAll().map(mapper::map))
            } catch (e: SQLException) {
                emitter.onError(e)
            }
        }
    }

    /**
     * The specified spending will have its id updated after a successful insert
     */
    fun createOrUpdate(spending: Spending): Completable {
        return Completable.create { emitter ->
            try {
                val dbSpending = mapper.map(spending)
                dao.createOrUpdate(dbSpending)
                emitter.onComplete()
                spending.id = dbSpending.id
                saveSavings(spending, dbSpending)
                saveCycleSpendings(spending, dbSpending)
            } catch (e: SQLException) {
                emitter.onError(e)
            }
        }
    }

    fun createOrUpdateSpendings(remoteSpendings: List<Spending>, remoteCategoryKey: String): Completable {
        return Completable.create { emitter ->
            val savedSpendings = dao.queryForAll()
            val savedRemoteCategories: MutableMap<String?, Int> = savedSpendings.associateBy {
                    mapper.mapSourceData(it.sourceData)?.get(remoteCategoryKey)
                }
                .mapValues {
                    it.value.id!!
                }
                .toMutableMap()
            try {
                remoteSpendings.forEach { remoteSpending ->
                    val remoteCategory = remoteSpending.sourceData!![remoteCategoryKey]

                    // find the saved spending that has the same remote category as the current remote spending
                    // Note: no two remote spending should have the same remote category, so there will only be one match
                    val id = savedRemoteCategories.remove(remoteCategory)

                    val dbSpending = mapper.map(remoteSpending).also {
                        it.id = id
                    }
                    if (id == null) {
                        dao.create(dbSpending)
                    } else {
                        dao.update(dbSpending)
                    }
                    saveSavings(remoteSpending, dbSpending)
                    saveCycleSpendings(remoteSpending, dbSpending)
                }
                // Disable leftovers (spendings that are no longer received)
                // This can happen when the user retroactively re-categorizes some spendings,
                // causing one or more of the categories to disappear
                savedSpendings.filter {
                    savedRemoteCategories.contains(it.type.toString())
                }.forEach {
                    it.enabled = false
                    dao.queryForId(it.id).cycleSpendings?.clear()
                    it.value = BigDecimal.ZERO
                    it.spent = BigDecimal.ZERO
                    dao.update(it)
                }
                emitter.onComplete()
            } catch (e: SQLException) {
                emitter.onError(Exception("Error creating/updating remote spendings", e))
            }
        }
    }

    private fun saveSavings(spending: Spending, dbSpending: DBSpending) {
        dao.assignEmptyForeignCollection(dbSpending, Contract.Spending.SAVINGS)
        dbSpending.savings?.let { foreignCollection ->
            foreignCollection.clear()
            spending.savings?.map { mapper.map(it, dbSpending) }?.asIterable()?.also {
                foreignCollection.addAll(it)
            }
        }
    }

    private fun saveCycleSpendings(spending: Spending, dbSpending: DBSpending) {
        dao.assignEmptyForeignCollection(dbSpending, Contract.Spending.CYCLE_SPENT)
        dbSpending.cycleSpendings?.let { foreignCollection ->
            foreignCollection.clear()
            spending.cycleSpendings?.map { mapper.map(it, dbSpending) }?.asIterable()?.also {
                foreignCollection.addAll(it)
            }
        }
    }

    fun deleteCycleSpendingBySpending(spending: Spending) {
        dao.queryForId(spending.id).cycleSpendings?.clear()
        spending.cycleSpendings = emptyList()
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
                dao.queryForAll().map {
                    Pair<DBSpending, Map<String, String>?>(
                        it,
                        it.sourceData?.let {
                            gson.fromJson<Map<String, String>>(it)
                        })
                }.filter {
                    it.second?.get(remoteCategoryKey)?.equals(category) ?: false
                }.forEach { emitter.onNext(mapper.map(it.first)) }
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
        val result = mutableListOf<Pair<DBSpending.Category, String>>()
        prefs.balanceReading?.let { reading ->
            val startDate = reading.date
            val endDate = prefs.estimateDate
            val totalExpense =
                calculateTotalBalanceExceptForCategory(omittedCategory = DBSpending.Category.INCOME, startDate = startDate, endDate = endDate)
            try {
                DBSpending.Category.values()
                    .map { Pair(it, calculateBalanceForCategory(it, startDate, endDate)) }
                    .filter { it.second.amount != 0f }
                    .sortedByDescending { Math.abs(it.second.amount) }
                    .forEach { (category, balance) ->
                        val name = balance.spending?.name
                            ?: category.name.substring(0, min(10, category.name.length)).toLowerCase().capitalize()
                        val amount: String = balance.amount.reverseSign()

                        result.add(Pair(category, if (category != DBSpending.Category.INCOME) {
                            val percent = balance.amount / totalExpense.amount * 100
                            "%1\$s: %2\$s (%3\$s%%)".format(name, amount, (-percent).reverseSign())
                        } else {
                            "%1\$s: %2\$s".format(name, amount)
                        }))
                    }
                val totalIncome = calculateBalanceForCategory(category = DBSpending.Category.INCOME, startDate = startDate, endDate = endDate)
                totalIncomeStr = "Tots. in: ${totalIncome.amount.reverseSign()}"
                totalExpenseStr = "Tots. out: ${totalExpense.amount.reverseSign()}"
            } catch (e: Throwable) {
                throw DomainException("Error calculating balance breakdown", e)
            }
        }
        return BalanceBreakdown(result.toTypedArray(), totalIncomeStr, totalExpenseStr)
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
                DBSpending.Category.values()
                    .map { Pair(it, calculateBalanceForCategory(it, startDate, endDate)) }
                    .filter { (it.second.target - it.second.amount) != 0f }
                    .sortedByDescending { Math.abs(it.second.target) }
                    .joinTo(buffer = buffer, separator = "\n", transform = { (category, balance) ->
                        val amount = balance.amount
                        val target = balance.target
                        val name = balance.spending?.name
                            ?: category.name.substring(0, Math.min(12, category.name.length)).toLowerCase().capitalize()
                        val saving = target - amount
                        val amountStr = "%1\$.2f".format(saving)
                        if (saving > 0) {
                            "%1\$s: %2\$s".format(name, amountStr)
                        } else {
                            hasNegAmounts = true
                            "%1\$s: %2\$s*".format(name, amountStr)
                        }
                    })
            } catch (e: IllegalArgumentException) {
                throw DomainException("Date of balance reading must not come after date of target estimate", e)
            }
            val balance = calculateTotalBalanceExceptForCategory(
                omittedCategory = DBSpending.Category.INCOME,
                startDate = startDate,
                endDate = endDate)
            val amount = balance.target - balance.amount
            buffer.append("\n-----------------\nTotal: ")
            buffer.append("%1\$.0f".format(amount))
            if (hasNegAmounts) {
                buffer.append(
                    "\n\n*Negative value means your average spending is now less then your limit in that category. Time to lower the limit?")
            }
        }
        return buffer.toString()
    }

    fun getBalanceBreakdownCategoryDetails(category: DBSpending.Category): String? {
        return prefs.balanceReading?.let { reading ->
            val startDate = reading.date!!
            val endDate = prefs.estimateDate
            val balance = calculateBalanceForCategory(category, startDate, endDate)
            val buffer = StringBuffer()
            var index = 0
            balance.spendingEvents?.joinTo(
                buffer = buffer,
                separator = "\n",
                transform = {
                    index++
                    val startStr = if (startDate <= it.start) {
                        "$index. ${it.start.formatDayMonth()}"
                    } else {
                        "$index. (${startDate.formatDayMonth()})"
                    }
                    val endStr = if (endDate >= it.end) {
                        "${it.end.formatDayMonthYear()} (%1\$.2f -> %2\$.2f)".format(it.amount, it.total)
                    } else {
                        "(${endDate.formatDayMonthYear()}) (%1\$.2f -> %2\$.2f)".format(it.amount, it.total)
                    }
                    "$startStr - $endStr"
                }).toString()
        }
    }

    /**
     * @param category for which the balance should be calculated. If null, all categories will be included
     * @param startDate from which the calculation should start. If null, the individual spending start-dates will be used
     * @param endDate up until which the calculations should go. If null, `today` is used
     */
    private fun calculateBalanceForCategory(category: DBSpending.Category?, startDate: LocalDate?,
                                            endDate: LocalDate): Balance {
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
    private fun calculateTotalBalanceExceptForCategory(omittedCategory: DBSpending.Category,
                                                       startDate: LocalDate?,
                                                       endDate: LocalDate): Balance {
        val builder: Where<DBSpending, Int> = dao.queryBuilder().where().notIn(Contract.Spending.TYPE, omittedCategory)
        return calculateBalance(builder, startDate, endDate)
    }

    private fun calculateBalance(builder: Where<DBSpending, Int>, startDate: LocalDate?, endDate: LocalDate): Balance {
        val spendingEvents = mutableListOf<SpendingEvent>()
        val totalBalance = Balance(0f, 0f, null, null)
        val spendings = dao.query(builder.prepare()).map(mapper::map)
        spendings.forEach { spending ->
            val balance = BalanceCalculator.getEstimatedBalance(spending, startDate, endDate)
            totalBalance.amount += balance.amount
            totalBalance.target += balance.target
            balance.spendingEvents?.forEach {
                spendingEvents.add(it)
            }
        }
        spendingEvents.sortBy { it.start }
        totalBalance.spendingEvents = spendingEvents.toTypedArray()
        if (spendings.size == 1) {
            totalBalance.spending = spendings[0]
        }
        return totalBalance
    }
}
