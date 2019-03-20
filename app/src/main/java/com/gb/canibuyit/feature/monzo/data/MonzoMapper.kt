package com.gb.canibuyit.feature.monzo.data

import com.gb.canibuyit.feature.monzo.MONZO_CATEGORY
import com.gb.canibuyit.feature.monzo.api.model.ApiMonzoLogin
import com.gb.canibuyit.feature.monzo.api.model.ApiMonzoTransaction
import com.gb.canibuyit.feature.monzo.api.model.ApiWebhook
import com.gb.canibuyit.feature.monzo.api.model.ApiWebhooks
import com.gb.canibuyit.feature.monzo.model.Login
import com.gb.canibuyit.feature.monzo.model.Transaction
import com.gb.canibuyit.feature.monzo.model.Webhook
import com.gb.canibuyit.feature.monzo.model.Webhooks
import com.gb.canibuyit.feature.project.data.Project
import com.gb.canibuyit.feature.spending.model.CycleSpent
import com.gb.canibuyit.feature.spending.model.Saving
import com.gb.canibuyit.feature.spending.model.Spending
import com.gb.canibuyit.feature.spending.model.div
import com.gb.canibuyit.feature.spending.model.firstCycleDay
import com.gb.canibuyit.feature.spending.model.lastCycleDay
import com.gb.canibuyit.feature.spending.model.ordinal
import com.gb.canibuyit.feature.spending.persistence.model.ApiSpending
import com.gb.canibuyit.model.SerializableMap
import com.gb.canibuyit.util.compare
import com.gb.canibuyit.util.max
import org.apache.commons.lang3.text.WordUtils
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonzoMapper @Inject constructor() {

    fun mapToLogin(apiMonzoLogin: ApiMonzoLogin): Login {
        val expiresAt = apiMonzoLogin.expires_in * 1000 + System.currentTimeMillis()
        return Login(apiMonzoLogin.access_token, apiMonzoLogin.refresh_token, expiresAt)
    }

    fun mapApiTransaction(apiMonzoTransaction: ApiMonzoTransaction): Transaction {
        val noteCategory = if (!apiMonzoTransaction.notes.isEmpty())
            ApiSpending.Category.values().firstOrNull {
                val noteCategoryStr = apiMonzoTransaction.notes.replace("-", "_")
                noteCategoryStr.equals(it.toString(), ignoreCase = true)
                        || noteCategoryStr.startsWith(it.toString() + " ", ignoreCase = true)
                        || noteCategoryStr.startsWith(it.toString() + "_", ignoreCase = true)
            }
        else
            null
        var category = mapRemoteCategory(noteCategory?.toString()?.toLowerCase()
                ?: apiMonzoTransaction.category)

        if (category == ApiSpending.Category.OTHER) {
            if (!apiMonzoTransaction.description.isEmpty()) {
                ApiSpending.Category.values().firstOrNull {
                    val noteCategoryStr = apiMonzoTransaction.description.replace("-", "_")
                    noteCategoryStr.equals(it.toString(), ignoreCase = true)
                            || noteCategoryStr.contains(it.toString() + " ", ignoreCase = true)
                            || noteCategoryStr.contains(it.toString() + "_", ignoreCase = true)
                }?.let {
                    category = mapRemoteCategory(it.toString())
                }
            }
        }

        val description = apiMonzoTransaction.description + if (apiMonzoTransaction.notes.isNotEmpty()) "\n" + apiMonzoTransaction.notes else ""
        return Transaction(apiMonzoTransaction.amount,
                ZonedDateTime.parse(apiMonzoTransaction.created).toLocalDate(),
                description,
                apiMonzoTransaction.id,
                category)
    }

    fun mapToSpending(category: ApiSpending.Category,
                      sortedTransactions: List<Transaction>,
                      savedSpending: Spending?,
                      projectSettings: Project,
                      startDate: LocalDate,
                      endDate: LocalDate): Spending {
        val categoryStr = WordUtils.capitalizeFully(category.toString().replace("\\_".toRegex(), " "))
        val name: String = if (projectSettings.namePinned) (savedSpending?.name ?: categoryStr) else categoryStr

        val type: ApiSpending.Category = if (projectSettings.categoryPinned) (savedSpending?.type ?: category) else category

        val cycle: ApiSpending.Cycle = getCycle(sortedTransactions, savedSpending, projectSettings)

        val cycleMultiplier: Int = if (projectSettings.cyclePinned) (savedSpending?.cycleMultiplier ?: 1) else 1

        val average: BigDecimal = getAverage(
                sortedTransactions,
                cycle,
                cycleMultiplier,
                savedSpending,
                projectSettings,
                startDate,
                endDate)

        val firstOccurrence: LocalDate = sortedTransactions[0].created
        val fromStartDate = if (projectSettings.whenPinned) (savedSpending?.fromStartDate ?: firstOccurrence) else firstOccurrence
        val fromEndDate = firstOccurrence.add(cycle, cycleMultiplier.toLong()).minusDays(1)
                .let { date ->
                    if (projectSettings.whenPinned) (savedSpending?.fromEndDate ?: date) else date
                }

        val transactionsByCycle: Map<Int, List<Transaction>> = sortedTransactions.groupBy { cycle.ordinal(it.created) }

        val spentByCycle = transactionsByCycle.map { (_, list) ->
            val firstDay = max(list[0].created.firstCycleDay(cycle), startDate)
            val lastDay = least(list[0].created.lastCycleDay(cycle), LocalDate.now())
            CycleSpent(
                    id = null,
                    spendingId = savedSpending?.id,
                    from = firstDay,
                    to = lastDay,
                    amount = list.sumBy { it.amount }.toBigDecimal().divide(100.toBigDecimal()),
                    count = list.size)
        }

        val shouldRecalculateSpentByCycle = (savedSpending?.spentByCycle == null
                || savedSpending.spentByCycle!!.isEmpty()
                || cycle != savedSpending.cycle
                || cycleMultiplier != savedSpending.cycleMultiplier
                || fromStartDate != savedSpending.fromStartDate
                || fromEndDate != savedSpending.fromEndDate)
                || !spentByCycle.compare(savedSpending.spentByCycle!!, spendByCycleComparator)

        val spent: BigDecimal = transactionsByCycle[cycle.ordinal(endDate)]
                ?.sumBy(Transaction::amount)?.toBigDecimal()
                ?.divide(100.toBigDecimal()) // cents to pounds
                ?: BigDecimal.ZERO

        val savings: List<Saving>? = getSavings(transactionsByCycle, cycle, savedSpending)

        return Spending(
                id = savedSpending?.id,
                targets = savedSpending?.targets,
                name = name,
                notes = savedSpending?.notes,
                type = type,
                value = average,
                total = sortedTransactions.sumBy(Transaction::amount).toBigDecimal(),
                fromStartDate = fromStartDate,
                fromEndDate = fromEndDate,
                occurrenceCount = savedSpending?.occurrenceCount,
                cycleMultiplier = cycleMultiplier,
                cycle = cycle,
                enabled = savedSpending?.enabled ?: type.defaultEnabled,
                spent = spent,
                spentByCycle = if (shouldRecalculateSpentByCycle) spentByCycle else savedSpending?.spentByCycle,
                savings = savings?.toTypedArray(),
                sourceData = SerializableMap<String, String>().apply { put(MONZO_CATEGORY, category.name.toLowerCase()) })
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

    private fun mapRemoteCategory(remoteCategory: String): ApiSpending.Category {
        return when (remoteCategory) {
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
                    ApiSpending.Category.valueOf(remoteCategory.replace("-", "_").toUpperCase())
                } catch (t: Throwable) {
                    ApiSpending.Category.OTHER
                }
            }
        }
    }

    private fun getCycle(sortedTransactions: List<Transaction>, savedSpending: Spending?, projectSettings: Project): ApiSpending.Cycle {
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

    private fun getOptimalCycle(sortedTransactions: List<Transaction>): ApiSpending.Cycle {
        var highestDistanceDays = 0L
        var distance: Long = 0

        sortedTransactions.indices.forEach { i ->
            if (i > 0 && { distance = sortedTransactions[i].created.toEpochDay() - sortedTransactions[i - 1].created.toEpochDay(); distance }() > highestDistanceDays) {
                highestDistanceDays = distance
            }
        }
        return when {
            highestDistanceDays <= 7 -> ApiSpending.Cycle.WEEKS
            highestDistanceDays <= 365 -> ApiSpending.Cycle.MONTHS
            else -> ApiSpending.Cycle.YEARS
        }
    }

    private fun getTarget(endDate: LocalDate, targets: Map<LocalDate, Int>?): Int? {
        val lastTargetDate = targets?.keys?.filter { it < endDate }?.max()
        return lastTargetDate?.let { targets[it] }
    }

    val spendByCycleComparator = Comparator<CycleSpent> { o1, o2 ->
        if (o1 === o2) return@Comparator 0
        if (o1.spendingId != o2.spendingId) return@Comparator 1
        if (o1.from != o2.from) return@Comparator 1
        if (o1.to != o2.to) return@Comparator 1
        if (o1.amount != o2.amount) return@Comparator 1
        if (o1.count != o2.count) return@Comparator 1
        0
    }
}