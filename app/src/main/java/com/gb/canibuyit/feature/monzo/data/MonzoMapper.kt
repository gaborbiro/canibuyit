package com.gb.canibuyit.feature.monzo.data

import com.gb.canibuyit.base.model.SerializableMap
import com.gb.canibuyit.feature.monzo.MONZO_CATEGORY
import com.gb.canibuyit.feature.monzo.api.model.*
import com.gb.canibuyit.feature.monzo.model.Login
import com.gb.canibuyit.feature.monzo.model.Transaction
import com.gb.canibuyit.feature.monzo.model.Webhook
import com.gb.canibuyit.feature.monzo.model.Webhooks
import com.gb.canibuyit.feature.project.data.Project
import com.gb.canibuyit.feature.spending.model.*
import com.gb.canibuyit.feature.spending.persistence.model.DBSpending
import com.gb.canibuyit.util.compare
import com.gb.canibuyit.util.max
import org.apache.commons.lang3.text.WordUtils
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.sumBy
import kotlin.math.roundToInt
import com.gb.canibuyit.util.sumBy as sumByBigDecimal

@Singleton
class MonzoMapper @Inject constructor() {

    fun mapToLogin(apiMonzoLogin: ApiMonzoLogin): Login {
        val expiresAt = apiMonzoLogin.expires_in * 1000 + System.currentTimeMillis()
        return Login(apiMonzoLogin.access_token, apiMonzoLogin.refresh_token, expiresAt)
    }

    fun mapApiTransaction(apiMonzoTransaction: ApiMonzoTransaction, pots: List<ApiPot>): Transaction {
        apiMonzoTransaction.apply {
            val category = listOf(
                identifyCategory(notes, pots), // highest priority
                identifyCategory(description, pots), // second highest priority
                mapCategory(category) // lowest priority
            )
                .mapNotNull { it }
                .firstOrNull()!!
            val notes = if (notes.isNotEmpty()) "\n" + notes else ""
            val description = description + notes
            return Transaction(
                amount = amount,
                created = ZonedDateTime.parse(created).toLocalDateTime(),
                description = description,
                id = id,
                category = category,
                originalCategory = this.category)
        }
    }

    private fun identifyCategory(text: String, pots: List<ApiPot>): DBSpending.Category? {
        return DBSpending.Category.values().firstOrNull {
            val categoryStr = text.replace("-", "_")
            categoryStr.equals(it.toString(), ignoreCase = true)
                || categoryStr.startsWith("$it ", ignoreCase = true)
                || categoryStr.startsWith("${it}_", ignoreCase = true)
        }
    }

//    private fun identifyPot(potId: String, pots: List<ApiPot>): DBSpending.Category? {
//        val potName = pots.find { it.id == potId }?.name
//        return potName?.let {
//            DBSpending.Category.values().firstOrNull {
//                val categoryStr = potName.replace("-", "_")
//                categoryStr.equals(it.toString(), ignoreCase = true)
//                    || categoryStr.contains("$it ", ignoreCase = true)
//                    || categoryStr.contains(it.toString() + "_", ignoreCase = true)
//            }
//        }
//    }

    fun mapToSpending(category: DBSpending.Category,
                      sortedTransactions: List<Transaction>,
                      savedSpending: Spending?,
                      projectSettings: Project,
                      startDate: LocalDateTime,
                      endDate: LocalDate): Spending {
        val categoryStr = WordUtils.capitalizeFully(category.toString().replace("\\_".toRegex(), " "))
        val finalName: String = if (projectSettings.namePinned) (savedSpending?.name
            ?: categoryStr) else categoryStr

        val finalCategory: DBSpending.Category = if (projectSettings.categoryPinned) (savedSpending?.type
            ?: category) else category

        val finalCycle: DBSpending.Cycle = getCycle(sortedTransactions, savedSpending, projectSettings)

        val finalCycleMultiplier: Int = if (projectSettings.cyclePinned) (savedSpending?.cycleMultiplier
            ?: 1) else 1

        val firstOccurrence = sortedTransactions[0].created.toLocalDate()
        val finalFromStartDate = if (projectSettings.whenPinned) (savedSpending?.fromStartDate
            ?: firstOccurrence) else firstOccurrence
        val finalFromEndDate = firstOccurrence.add(finalCycle, finalCycleMultiplier.toLong()).minusDays(1)
            .let { date ->
                if (projectSettings.whenPinned) (savedSpending?.fromEndDate ?: date) else date
            }

        val transactionsByCycle: Map<Int, List<Transaction>> = sortedTransactions.groupBy { finalCycle.ordinal(it.created.toLocalDate()) }

        val cycleSpendings: List<CycleSpending> = transactionsByCycle.map { (_, transactionsInCycle) ->
            // kinda redundant, date of first transaction will never be before startDate
            val firstDay = max(transactionsInCycle[0].created.toLocalDate().firstCycleDay(finalCycle), startDate.toLocalDate())
            // Kinda random. What should be the end date of the last entry? Last cycle day is a bit misleading,
            // it's like claiming that that's all the money that's gonna be spent in the current cycle.
            // Cutting it off with LocalDate.now() is less misleading.
            val lastDay = least(transactionsInCycle[0].created.toLocalDate().lastCycleDay(finalCycle), LocalDate.now())
            CycleSpending(
                id = null,
                spendingId = savedSpending?.id,
                from = firstDay,
                to = lastDay,
                amount = transactionsInCycle.sumBy { it.amount }.toBigDecimal().divide(100.toBigDecimal()),
                target = getTarget(transactionsInCycle, savedSpending?.targets, finalCycle),
                count = transactionsInCycle.size)
        }

        val finalAverage: BigDecimal = getAverage(
            cycleSpendings,
            finalCycle,
            finalCycleMultiplier,
            savedSpending,
            projectSettings,
            startDate,
            endDate)

        val shouldRecalculateSpentByCycle = (
            savedSpending?.cycleSpendings == null
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
                put(MONZO_CATEGORY, category.label)
            })
    }

    fun mapToWebhooks(apiWebhooks: ApiWebhooks): Webhooks {
        return Webhooks(apiWebhooks.webhooks.map { mapToWebhook(it) })
    }

    private fun mapToWebhook(apiWebhook: ApiWebhook): Webhook {
        return Webhook(apiWebhook.id, apiWebhook.url)
    }

    fun LocalDate.add(cycle: DBSpending.Cycle, amount: Long): LocalDate {
        return when (cycle) {
            DBSpending.Cycle.DAYS -> this.plusDays(amount)
            DBSpending.Cycle.WEEKS -> this.plusWeeks(amount)
            DBSpending.Cycle.MONTHS -> this.plusMonths(amount)
            DBSpending.Cycle.YEARS -> this.plusYears(amount)
        }
    }

    private fun mapCategory(monzoCategory: String): DBSpending.Category {
        return when (monzoCategory) {
            "mondo" -> DBSpending.Category.INCOME
            "eating_out" -> DBSpending.Category.FOOD
            "bills" -> DBSpending.Category.UTILITIES
            "shopping" -> DBSpending.Category.LUXURY
            "holidays" -> DBSpending.Category.VACATION
            "pot" -> DBSpending.Category.SAVINGS
            "family" -> DBSpending.Category.ACCOMMODATION
            "charity" -> DBSpending.Category.DONATIONS_GIVEN
            else -> {
                try {
                    DBSpending.Category.valueOf(monzoCategory.replace("-", "_").toUpperCase())
                } catch (t: Throwable) {
                    t.printStackTrace()
                    DBSpending.Category.GENERAL
                }
            }
        }
    }

    private fun getCycle(sortedTransactions: List<Transaction>, savedSpending: Spending?,
                         projectSettings: Project): DBSpending.Cycle {
        val optimalCycle = getOptimalCycle(sortedTransactions)
        return if (projectSettings.cyclePinned) (savedSpending?.cycle
            ?: optimalCycle) else optimalCycle
    }

    private fun getSavings(transactionsByCycle: Map<*, List<Transaction>>, cycle: DBSpending.Cycle,
                           savedSpending: Spending?): List<Saving>? {
        val savings = transactionsByCycle.mapNotNull saving@{
            val transactionsForCycle: List<Transaction> = it.value
            getTarget(transactionsForCycle, savedSpending?.targets, cycle)?.let { target ->
                val saving = transactionsForCycle.sumBy(Transaction::amount)
                    .toBigDecimal()
                    .divide(100.toBigDecimal())
                    .minus(target.toBigDecimal())
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

    private fun getAverage(cycleSpendings: List<CycleSpending>,
                           cycle: DBSpending.Cycle,
                           cycleMultiplier: Int,
                           savedSpending: Spending?,
                           projectSettings: Project,
                           startDate: LocalDateTime,
                           endDate: LocalDate): BigDecimal {
        val savedAverage = if (projectSettings.averagePinned) savedSpending?.value else null
        return savedAverage ?: cycleSpendings
            .sumByBigDecimal { it.amount }
            .divide((Pair(startDate.toLocalDate(), endDate) / cycle / cycleMultiplier).roundToInt().toBigDecimal(), RoundingMode.DOWN)
    }

    private fun getOptimalCycle(sortedTransactions: List<Transaction>): DBSpending.Cycle {
        var highestDistanceDays = 0L
        var distance: Long = 0

        sortedTransactions.indices.forEach { i ->
            if (i > 0 && {
                    distance =
                        sortedTransactions[i].created.toLocalDate().toEpochDay() - sortedTransactions[i - 1].created.toLocalDate().toEpochDay(); distance
                }() > highestDistanceDays) {
                highestDistanceDays = distance
            }
        }
        return when {
            highestDistanceDays <= 7 -> DBSpending.Cycle.WEEKS
            highestDistanceDays <= 365 -> DBSpending.Cycle.MONTHS
            else -> DBSpending.Cycle.YEARS
        }
    }

    private fun getTarget(transactionsForCycle: List<Transaction>, targets: Map<LocalDate, Int>?, cycle: DBSpending.Cycle): Int? {
        val lastCycleDay = transactionsForCycle.maxBy(Transaction::created)?.created!!.toLocalDate().lastCycleDay(cycle)
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