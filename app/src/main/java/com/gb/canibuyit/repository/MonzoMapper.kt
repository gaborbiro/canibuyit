package com.gb.canibuyit.repository

import com.gb.canibuyit.api.model.ApiLogin
import com.gb.canibuyit.api.model.ApiTransaction
import com.gb.canibuyit.api.model.ApiWebhook
import com.gb.canibuyit.api.model.ApiWebhooks
import com.gb.canibuyit.db.model.ApiSpending
import com.gb.canibuyit.interactor.Project
import com.gb.canibuyit.model.Login
import com.gb.canibuyit.model.Saving
import com.gb.canibuyit.model.SerializableMap
import com.gb.canibuyit.model.Spending
import com.gb.canibuyit.model.Transaction
import com.gb.canibuyit.model.Webhook
import com.gb.canibuyit.model.Webhooks
import com.gb.canibuyit.model.lastCycleDay
import com.gb.canibuyit.model.ordinal
import com.gb.canibuyit.model.span
import org.apache.commons.lang3.text.WordUtils
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
                    || noteCategoryStr.contains(it.toString() + " ", ignoreCase = true)
                    || noteCategoryStr.contains(it.toString() + "_", ignoreCase = true)
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
            ZonedDateTime.parse(apiTransaction.created),
            description,
            apiTransaction.id,
            category)
    }

    fun mapToSpending(category: ApiSpending.Category, sortedTransactions: List<Transaction>, savedSpending: Spending?, projectSettings: Project): Spending? {
        val categoryStr = WordUtils.capitalizeFully(category.toString().replace("\\_".toRegex(), " "))
        val name: String = if (projectSettings.namePinned) (savedSpending?.name ?: categoryStr) else categoryStr

        val type: ApiSpending.Category = if (projectSettings.categoryPinned) (savedSpending?.type ?: category) else category

        val cycle: ApiSpending.Cycle = getCycle(sortedTransactions, savedSpending, projectSettings)

        val cycleMultiplier: Int = if (projectSettings.cyclePinned) (savedSpending?.cycleMultiplier ?: 1) else 1

        val average: Double = getAverage(sortedTransactions, cycle, cycleMultiplier, savedSpending, projectSettings)

        val firstOccurrence: LocalDate = sortedTransactions[0].created.toLocalDate()

        val fromEndDate: LocalDate = firstOccurrence.add(cycle, cycleMultiplier.toLong()).minusDays(1)

        val (spent: Double, savings: List<Saving>?) = sortedTransactions.groupBy { cycle.ordinal(it.created.toLocalDate()) }
            .let { transactionsGroupedByCycle: Map<Int, List<Transaction>> ->
                val spent = transactionsGroupedByCycle[cycle.ordinal(LocalDate.now())]
                    ?.sumBy(Transaction::amount)
                    ?.div(100.0)
                    ?: 0.0 // cents to pounds
                val savings: List<Saving>? = getSavings(transactionsGroupedByCycle, cycle, savedSpending)
                return@let Pair(spent, savings)
            }

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

    private fun getCycle(sortedTransactions: List<Transaction>, savedSpending: Spending?, projectSettings: Project): ApiSpending.Cycle {
        val optimalCycle = getOptimalCycle(sortedTransactions)
        return if (projectSettings.cyclePinned) (savedSpending?.cycle
            ?: optimalCycle) else optimalCycle
    }

    private fun getSavings(transactionsGroupedByCycle: Map<Int, List<Transaction>>, cycle: ApiSpending.Cycle, savedSpending: Spending?): List<Saving>? {
        val savings = transactionsGroupedByCycle.mapNotNull saving@{
            val transactionsForCycle = it.value
            val lastCycleDay = transactionsForCycle.maxBy(Transaction::created)?.created!!.toLocalDate().lastCycleDay(cycle)

            if (lastCycleDay <= LocalDate.now()) { // a cycle must end before its savings can be recorded
                val target = getTarget(lastCycleDay, savedSpending?.targets)
                val saving = target?.let { transactionsForCycle.sumBy(Transaction::amount).div(100.0).minus(target) }
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

    private fun getAverage(sortedTransactions: List<Transaction>, cycle: ApiSpending.Cycle, cycleMultiplier: Int, savedSpending: Spending?, projectSettings: Project): Double {
        val savedAverage = if (projectSettings.averagePinned) savedSpending?.value else null
        return savedAverage ?: sortedTransactions
            .sumBy(Transaction::amount)
            .div(Pair(sortedTransactions[0].created.toLocalDate(), LocalDate.now()).span(cycle) / cycleMultiplier)
            .div(100.0) // cents to pounds
    }

    private fun getOptimalCycle(sortedTransactions: List<Transaction>): ApiSpending.Cycle {
        var highestDistanceDays = 0L
        val sortedDates = sortedTransactions.map { it.created.toLocalDate().toEpochDay() }
        var distance: Long = 0

        sortedDates.indices.forEach { i ->
            if (i > 0 && { distance = sortedDates[i] - sortedDates[i - 1]; distance }() > highestDistanceDays) {
                highestDistanceDays = distance
            }
        }
        return when {
            highestDistanceDays <= 7 -> ApiSpending.Cycle.WEEKS
            highestDistanceDays <= 365 -> ApiSpending.Cycle.MONTHS
            else -> ApiSpending.Cycle.YEARS
        }
    }

    private fun getTarget(endDate: LocalDate, targets: Map<LocalDate, Double>?): Double? {
        val lastTargetDate = targets?.keys?.filter { it < endDate }?.max()
        return lastTargetDate?.let { targets[it] }
    }
}