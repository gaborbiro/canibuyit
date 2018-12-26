package com.gb.canibuyit.repository

import com.gb.canibuyit.api.model.ApiLogin
import com.gb.canibuyit.api.model.ApiTransaction
import com.gb.canibuyit.api.model.ApiWebhook
import com.gb.canibuyit.api.model.ApiWebhooks
import com.gb.canibuyit.db.model.ApiSpending
import com.gb.canibuyit.interactor.Project
import com.gb.canibuyit.model.Cycle
import com.gb.canibuyit.model.CycleSpent
import com.gb.canibuyit.model.Login
import com.gb.canibuyit.model.Saving
import com.gb.canibuyit.model.SerializableMap
import com.gb.canibuyit.model.Spending
import com.gb.canibuyit.model.Transaction
import com.gb.canibuyit.model.Webhook
import com.gb.canibuyit.model.Webhooks
import com.gb.canibuyit.model.div
import com.gb.canibuyit.model.firstCycleDay
import com.gb.canibuyit.model.lastCycleDay
import com.gb.canibuyit.model.ordinal
import com.gb.canibuyit.util.doIfBoth
import org.apache.commons.lang3.text.WordUtils
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonzoMapper @Inject constructor() {

    fun mapToLogin(apiLogin: ApiLogin): Login {
        val expiresAt = apiLogin.expires_in * 1000 + System.currentTimeMillis()
        return Login(apiLogin.access_token, apiLogin.refresh_token, expiresAt)
    }

    fun mapToTransaction(apiTransaction: ApiTransaction): Transaction {
        val noteCategory = if (!apiTransaction.notes.isEmpty())
            ApiSpending.Category.values().firstOrNull {
                val noteCategoryStr = apiTransaction.notes.replace("-", "_")
                noteCategoryStr.equals(it.toString(), ignoreCase = true)
                        || noteCategoryStr.startsWith(it.toString() + " ", ignoreCase = true)
                        || noteCategoryStr.startsWith(it.toString() + "_", ignoreCase = true)
            }
        else
            null
        var category = mapApiCategory(noteCategory?.toString()?.toLowerCase()
                ?: apiTransaction.category)

        if (category == ApiSpending.Category.OTHER) {
            if (!apiTransaction.description.isEmpty()) {
                ApiSpending.Category.values().firstOrNull {
                    val noteCategoryStr = apiTransaction.description.replace("-", "_")
                    noteCategoryStr.equals(it.toString(), ignoreCase = true)
                            || noteCategoryStr.contains(it.toString() + " ", ignoreCase = true)
                            || noteCategoryStr.contains(it.toString() + "_", ignoreCase = true)
                }?.let {
                    category = mapApiCategory(it.toString())
                }
            }
        }

        val description = apiTransaction.description + if (apiTransaction.notes.isNotEmpty()) "\n" + apiTransaction.notes else ""
        return Transaction(apiTransaction.amount,
                ZonedDateTime.parse(apiTransaction.created).toLocalDate(),
                description,
                apiTransaction.id,
                category)
    }

    fun mapToSpending(category: ApiSpending.Category,
                      sortedTransactions: List<Transaction>,
                      savedSpending: Spending?,
                      projectSettings: Project,
                      startDate: LocalDate,
                      endDate: LocalDate): Spending {
        Pair(sortedTransactions.firstOrNull()?.created,
                sortedTransactions.lastOrNull()?.created).doIfBoth { (firstDate, lastDate) ->
            if (firstDate < startDate || lastDate > endDate) {
                throw IllegalArgumentException("List of transactions ($category) is out of bounds: start: $startDate first: $firstDate last: $lastDate end: $endDate")
            }
        }
        val categoryStr = WordUtils.capitalizeFully(category.toString().replace("\\_".toRegex(), " "))
        val name: String = if (projectSettings.namePinned) (savedSpending?.name ?: categoryStr) else categoryStr

        val type: ApiSpending.Category = if (projectSettings.categoryPinned) (savedSpending?.type ?: category) else category

        val cycle: Cycle = getCycle(sortedTransactions, savedSpending, projectSettings)

        val cycleMultiplier: Int = if (projectSettings.cyclePinned) (savedSpending?.cycleMultiplier ?: 1) else 1

        val average: BigDecimal = getAverage(
                sortedTransactions,
                cycle.apiCycle,
                cycleMultiplier,
                savedSpending,
                projectSettings,
                startDate,
                endDate)

        val firstOccurrence: LocalDate = sortedTransactions[0].created

        val fromEndDate: LocalDate = firstOccurrence.add(cycle.apiCycle, cycleMultiplier.toLong())
                .minusDays(1)

        val transactionsByCycle: Map<Int, List<Transaction>> = sortedTransactions.groupBy { cycle.apiCycle.ordinal(it.created) }

        val spentByCycle = transactionsByCycle.map { (pair, list) ->
            val firstDay = list[0].created.firstCycleDay(cycle.apiCycle)
            val lastDay = least(list[0].created.lastCycleDay(cycle.apiCycle), LocalDate.now())
            CycleSpent(
                    id = null,
                    spendingId = savedSpending?.id,
                    from = firstDay,
                    to = lastDay,
                    amount = list.sumBy { it.amount }.toBigDecimal().divide(100.toBigDecimal()),
                    count = list.size,
                    enabled = true)
        }
        val spent: BigDecimal = spentByCycle.last().amount
        val savings: List<Saving>? = getSavings(transactionsByCycle, cycle.apiCycle, savedSpending)

        return Spending(
                id = savedSpending?.id,
                targets = savedSpending?.targets,
                name = name,
                notes = savedSpending?.notes,
                type = type,
                value = average,
                fromStartDate = if (projectSettings.whenPinned) (savedSpending?.fromStartDate ?: firstOccurrence) else firstOccurrence,
                fromEndDate = if (projectSettings.whenPinned) (savedSpending?.fromEndDate ?: fromEndDate) else fromEndDate,
                occurrenceCount = savedSpending?.occurrenceCount,
                cycleMultiplier = cycleMultiplier,
                cycle = cycle,
                enabled = savedSpending?.enabled ?: type.defaultEnabled,
                spent = spent,
                spentByCycle = spentByCycle,
                savings = savings?.toTypedArray(),
                sourceData = SerializableMap<String, String>().apply { put(ApiSpending.SOURCE_MONZO_CATEGORY, category.name.toLowerCase()) })
    }

    fun mapToWebhooks(apiWebhooks: ApiWebhooks): Webhooks {
        return Webhooks(apiWebhooks.webhooks.map { mapToWebhook(it) })
    }

    private fun mapToWebhook(apiWebhook: ApiWebhook): Webhook {
        return Webhook(apiWebhook.id, apiWebhook.url)
    }

    fun LocalDate.add(cycle: ApiSpending.Cycle, amount: Long): LocalDate {
        return when (cycle) {
            ApiSpending.Cycle.DAYS -> this.plusDays(amount)
            ApiSpending.Cycle.WEEKS -> this.plusWeeks(amount)
            ApiSpending.Cycle.MONTHS -> this.plusMonths(amount)
            ApiSpending.Cycle.YEARS -> this.plusYears(amount)
        }
    }

    private fun mapApiCategory(apiCategory: String): ApiSpending.Category {
        return when (apiCategory) {
            "mondo" -> ApiSpending.Category.INCOME
            "general" -> ApiSpending.Category.OTHER
            "eating_out" -> ApiSpending.Category.FOOD
            "expenses" -> ApiSpending.Category.EXPENSES
            "transport" -> ApiSpending.Category.TRANSPORTATION
            "cash" -> ApiSpending.Category.CASH
            "bills" -> ApiSpending.Category.UTILITIES
            "entertainment" -> ApiSpending.Category.ENTERTAINMENT
            "shopping" -> ApiSpending.Category.LUXURY
            "holidays" -> ApiSpending.Category.VACATION
            "groceries" -> ApiSpending.Category.GROCERIES
            "pot" -> ApiSpending.Category.SAVINGS
            else -> {
                try {
                    ApiSpending.Category.valueOf(apiCategory.replace("-", "_").toUpperCase())
                } catch (t: Throwable) {
                    ApiSpending.Category.OTHER
                }
            }
        }
    }

    private fun getCycle(sortedTransactions: List<Transaction>, savedSpending: Spending?, projectSettings: Project): Cycle {
        val optimalCycle = getOptimalCycle(sortedTransactions)
        return if (projectSettings.cyclePinned) (savedSpending?.cycle ?: optimalCycle) else optimalCycle
    }

    private fun getSavings(transactionsByCycle: Map<*, List<Transaction>>, cycle: ApiSpending.Cycle, savedSpending: Spending?): List<Saving>? {
        val savings = transactionsByCycle.mapNotNull saving@{
            val transactionsForCycle = it.value
            val lastCycleDay = transactionsForCycle.maxBy(Transaction::created)?.created!!
                    .lastCycleDay(cycle)

            if (lastCycleDay <= LocalDate.now()) { // a cycle must end before its savings can be recorded
                val target = getTarget(lastCycleDay, savedSpending?.targets)
                val saving = target?.let {
                    transactionsForCycle.sumBy(Transaction::amount).div(100.0).minus(target)
                }
                if (saving != null) {
                    return@saving Saving(null, savedSpending?.id, saving, lastCycleDay, target)
                } else {
                    return@saving null
                }
            } else {
                return@saving null
            }
        }
        return if (savings.isNotEmpty()) {
            savings
        } else {
            null
        }
    }

    private fun getAverage(sortedTransactions: List<Transaction>,
                           cycle: ApiSpending.Cycle,
                           cycleMultiplier: Int,
                           savedSpending: Spending?,
                           projectSettings: Project,
                           startDate: LocalDate,
                           endDate: LocalDate): BigDecimal {
        val savedAverage = if (projectSettings.averagePinned) savedSpending?.value else null
        return savedAverage ?: sortedTransactions
                .sumBy(Transaction::amount).toBigDecimal()
                .divide((Pair(startDate, endDate) / cycle / cycleMultiplier).toBigDecimal(), RoundingMode.DOWN)
                .divide(100.toBigDecimal()) // cents to pounds
    }

    private fun getOptimalCycle(sortedTransactions: List<Transaction>): Cycle {
        var highestDistanceDays = 0L
        var distance: Long = 0

        sortedTransactions.indices.forEach { i ->
            if (i > 0 && { distance = sortedTransactions[i].created.toEpochDay() - sortedTransactions[i - 1].created.toEpochDay(); distance }() > highestDistanceDays) {
                highestDistanceDays = distance
            }
        }
        return when {
            highestDistanceDays <= 7 -> Cycle.Weeks(sortedTransactions.size)
            highestDistanceDays <= 365 -> Cycle.Months(sortedTransactions.size)
            else -> Cycle.Years(sortedTransactions.size)
        }
    }

    private fun getTarget(endDate: LocalDate, targets: Map<LocalDate, Int>?): Int? {
        val lastTargetDate = targets?.keys?.filter { it < endDate }?.max()
        return lastTargetDate?.let { targets[it] }
    }
}