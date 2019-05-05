package com.gb.canibuyit.feature.monzo.data

import com.gb.canibuyit.base.model.SerializableMap
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
import com.gb.canibuyit.feature.spending.model.CycleSpending
import com.gb.canibuyit.feature.spending.model.Saving
import com.gb.canibuyit.feature.spending.model.Spending
import com.gb.canibuyit.feature.spending.model.div
import com.gb.canibuyit.feature.spending.model.firstCycleDay
import com.gb.canibuyit.feature.spending.model.lastCycleDay
import com.gb.canibuyit.feature.spending.model.ordinal
import com.gb.canibuyit.feature.spending.persistence.model.ApiSpending
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
        apiMonzoTransaction.apply {
            val category = listOf(
                identifyCategory(notes), // highest priority
                identifyCategory(description), // second highest priority
                mapCategory(category) // lowest priority
            )
                .mapNotNull { it }
                .firstOrNull()!!
            val notes = if (notes.isNotEmpty()) "\n" + notes else ""
            val description = description + notes
            return Transaction(
                amount = amount,
                created = ZonedDateTime.parse(created).toLocalDate(),
                description = description,
                id = id,
                category = category)
        }
    }

    private fun identifyCategory(remoteCategory: String): ApiSpending.Category? {
        val category = ApiSpending.Category.values().firstOrNull {
            val noteCategoryStr = remoteCategory.replace("-", "_")
            noteCategoryStr.equals(it.toString(), ignoreCase = true)
                || noteCategoryStr.contains("$it ", ignoreCase = true)
                || noteCategoryStr.contains(it.toString() + "_", ignoreCase = true)
        }
        return if (category == ApiSpending.Category.POT) ApiSpending.Category.SAVINGS else category
    }

    fun mapToSpending(category: ApiSpending.Category,
                      sortedTransactions: List<Transaction>,
                      savedSpending: Spending?,
                      projectSettings: Project,
                      startDate: LocalDate,
                      endDate: LocalDate): Spending {
        val categoryStr =
            WordUtils.capitalizeFully(category.toString().replace("\\_".toRegex(), " "))
        val finalName: String =
            if (projectSettings.namePinned) (savedSpending?.name ?: categoryStr) else categoryStr

        val finalCategory: ApiSpending.Category =
            if (projectSettings.categoryPinned) (savedSpending?.type ?: category) else category

        val finalCycle: ApiSpending.Cycle =
            getCycle(sortedTransactions, savedSpending, projectSettings)

        val finalCycleMultiplier: Int =
            if (projectSettings.cyclePinned) (savedSpending?.cycleMultiplier ?: 1) else 1

        val finalAverage: BigDecimal = getAverage(
            sortedTransactions,
            finalCycle,
            finalCycleMultiplier,
            savedSpending,
            projectSettings,
            startDate,
            endDate)

        val firstOccurrence: LocalDate = sortedTransactions[0].created
        val finalFromStartDate = if (projectSettings.whenPinned) (savedSpending?.fromStartDate
            ?: firstOccurrence) else firstOccurrence
        val finalFromEndDate =
            firstOccurrence.add(finalCycle, finalCycleMultiplier.toLong()).minusDays(1)
                .let { date ->
                    if (projectSettings.whenPinned) (savedSpending?.fromEndDate ?: date) else date
                }

        val transactionsByCycle: Map<Int, List<Transaction>> =
            sortedTransactions.groupBy { finalCycle.ordinal(it.created) }

        val cycleSpendings = transactionsByCycle.map { (_, list) ->
            val firstDay = max(list[0].created.firstCycleDay(finalCycle), startDate)
            val lastDay = least(list[0].created.lastCycleDay(finalCycle), LocalDate.now())
            CycleSpending(
                id = null,
                spendingId = savedSpending?.id,
                from = firstDay,
                to = lastDay,
                amount = list.sumBy { it.amount }.toBigDecimal().divide(100.toBigDecimal()),
                target = getTarget(list, savedSpending?.targets, finalCycle),
                count = list.size)
        }

        val shouldRecalculateSpentByCycle = (savedSpending?.cycleSpendings == null
            || savedSpending.cycleSpendings!!.isEmpty()
            || finalCycle != savedSpending.cycle
            || finalCycleMultiplier != savedSpending.cycleMultiplier
            || finalFromStartDate != savedSpending.fromStartDate
            || finalFromEndDate != savedSpending.fromEndDate)
            || !cycleSpendings.compare(savedSpending.cycleSpendings!!, cycleSpendingComparator)

        val spent: BigDecimal = transactionsByCycle[finalCycle.ordinal(endDate)]
            ?.sumBy(Transaction::amount)?.toBigDecimal()
            ?.divide(100.toBigDecimal()) // cents to pounds
            ?: BigDecimal.ZERO

        val savings: List<Saving>? = getSavings(transactionsByCycle, finalCycle, savedSpending)

        return Spending(
            id = savedSpending?.id,
            targets = savedSpending?.targets,
            name = finalName,
            notes = savedSpending?.notes,
            type = finalCategory,
            value = finalAverage,
            total = sortedTransactions.sumBy(Transaction::amount).toBigDecimal(),
            fromStartDate = finalFromStartDate,
            fromEndDate = finalFromEndDate,
            occurrenceCount = savedSpending?.occurrenceCount,
            cycleMultiplier = finalCycleMultiplier,
            cycle = finalCycle,
            enabled = savedSpending?.enabled ?: finalCategory.defaultEnabled,
            spent = spent,
            cycleSpendings = if (shouldRecalculateSpentByCycle) cycleSpendings else savedSpending?.cycleSpendings,
            savings = savings?.toTypedArray(),
            sourceData = SerializableMap<String, String>().apply {
                put(MONZO_CATEGORY, category.name.toLowerCase())
            })
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

    private fun mapCategory(monzoCategory: String): ApiSpending.Category {
        return when (monzoCategory) {
            "mondo" -> ApiSpending.Category.INCOME
            "general" -> ApiSpending.Category.OTHER
            "eating_out" -> ApiSpending.Category.FOOD
            "transport" -> ApiSpending.Category.TRANSPORTATION
            "bills" -> ApiSpending.Category.UTILITIES
            "shopping" -> ApiSpending.Category.LUXURY
            "holidays" -> ApiSpending.Category.VACATION
            "pot" -> ApiSpending.Category.SAVINGS
            "family" -> ApiSpending.Category.ACCOMMODATION
            "charity" -> ApiSpending.Category.GIFTS_GIVEN
            else -> {
                try {
                    ApiSpending.Category.valueOf(monzoCategory.replace("-", "_").toUpperCase())
                } catch (t: Throwable) {
                    t.printStackTrace()
                    ApiSpending.Category.OTHER
                }
            }
        }
    }

    private fun getCycle(sortedTransactions: List<Transaction>, savedSpending: Spending?,
                         projectSettings: Project): ApiSpending.Cycle {
        val optimalCycle = getOptimalCycle(sortedTransactions)
        return if (projectSettings.cyclePinned) (savedSpending?.cycle
            ?: optimalCycle) else optimalCycle
    }

    private fun getSavings(transactionsByCycle: Map<*, List<Transaction>>, cycle: ApiSpending.Cycle,
                           savedSpending: Spending?): List<Saving>? {
        val savings = transactionsByCycle.mapNotNull saving@{
            val transactionsForCycle: List<Transaction> = it.value
            getTarget(transactionsForCycle, savedSpending?.targets, cycle)?.let { target ->
                val saving = transactionsForCycle.sumBy(Transaction::amount).div(100.0).minus(target)
                return@saving Saving(null, savedSpending?.id, saving, LocalDate.now(), target)
            } ?: run {
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
            .divide((Pair(startDate, endDate) / cycle / cycleMultiplier).toBigDecimal(),
                RoundingMode.DOWN)
            .divide(100.toBigDecimal()) // cents to pounds
    }

    private fun getOptimalCycle(sortedTransactions: List<Transaction>): ApiSpending.Cycle {
        var highestDistanceDays = 0L
        var distance: Long = 0

        sortedTransactions.indices.forEach { i ->
            if (i > 0 && {
                    distance =
                        sortedTransactions[i].created.toEpochDay() - sortedTransactions[i - 1].created.toEpochDay(); distance
                }() > highestDistanceDays) {
                highestDistanceDays = distance
            }
        }
        return when {
            highestDistanceDays <= 7 -> ApiSpending.Cycle.WEEKS
            highestDistanceDays <= 365 -> ApiSpending.Cycle.MONTHS
            else -> ApiSpending.Cycle.YEARS
        }
    }

    private fun getTarget(transactionsForCycle: List<Transaction>, targets: Map<LocalDate, Int>?, cycle: ApiSpending.Cycle): Int? {
        val lastCycleDay = transactionsForCycle.maxBy(Transaction::created)?.created!!.lastCycleDay(cycle)
        return if (lastCycleDay <= LocalDate.now()) { // ongoing cycles don't have savings calculated on them
            val lastTargetDate = targets?.keys?.filter { it < lastCycleDay }?.max()
            lastTargetDate?.let { targets[it] }
        } else {
            null
        }
    }

    private val cycleSpendingComparator = Comparator<CycleSpending> { cycleSpent1, o2 ->
        if (cycleSpent1 === o2) return@Comparator 0
        if (cycleSpent1.spendingId != o2.spendingId) return@Comparator 1
        if (cycleSpent1.from != o2.from) return@Comparator 1
        if (cycleSpent1.to != o2.to) return@Comparator 1
        if (cycleSpent1.amount != o2.amount) return@Comparator 1
        if (cycleSpent1.count != o2.count) return@Comparator 1
        0
    }
}