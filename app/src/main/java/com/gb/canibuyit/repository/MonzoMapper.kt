package com.gb.canibuyit.repository

import com.gb.canibuyit.api.model.ApiLogin
import com.gb.canibuyit.api.model.ApiTransaction
import com.gb.canibuyit.api.model.ApiWebhook
import com.gb.canibuyit.api.model.ApiWebhooks
import com.gb.canibuyit.db.model.ApiSpending
import com.gb.canibuyit.interactor.Project
import com.gb.canibuyit.model.*
import com.gb.canibuyit.util.nonNullAndTrue
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
            ApiSpending.Category.values().firstOrNull { apiTransaction.notes.replace("-", "_").contains(it.toString(), ignoreCase = true) }
        else
            null
        val category = mapApiCategory(noteCategory?.toString()?.toLowerCase()
            ?: apiTransaction.category)
        val description = apiTransaction.description + if (apiTransaction.notes.isNotEmpty()) "\n" + apiTransaction.notes else ""
        return Transaction(apiTransaction.amount,
            ZonedDateTime.parse(apiTransaction.created),
            description,
            apiTransaction.id,
            category)
    }

    fun mapToSpending(category: ApiSpending.Category, transactions: List<Transaction>, savedSpending: Spending?, projectSettings: Project, since: LocalDate? = null): Spending? {
        val cycle: ApiSpending.Cycle = nonNullAndTrue(savedSpending?.cycle, projectSettings.cyclePinned)
            ?: getOptimalCycle(transactions)

        val transactionsGroupedByCycle: Map<Int, List<Transaction>> = transactions.groupBy { cycle.ordinal(it.created.toLocalDate()) }
        var savings: List<Saving>? = transactionsGroupedByCycle.mapNotNull saving@{
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
        if (savings?.isNotEmpty() == false) {
            savings = null
        }

        val type = nonNullAndTrue(savedSpending?.type, projectSettings.categoryPinned) ?: category
        val firstOccurrence: LocalDate = transactions.minBy { it.created }!!.created.toLocalDate()
        val cycleMultiplier = nonNullAndTrue(savedSpending?.cycleMultiplier, projectSettings.cyclePinned)
            ?: 1
        val average = nonNullAndTrue(savedSpending?.value, projectSettings.averagePinned)
            ?: transactions
                .sumBy { it.amount }
                .div(Pair(since
                    ?: transactions.minBy { it.created }!!.created.toLocalDate(), LocalDate.now()).span(cycle) / cycleMultiplier)
                .div(100.0) // cents to pounds
        return Spending(
            id = savedSpending?.id,
            targets = nonNullAndTrue(savedSpending?.targets),
            name = nonNullAndTrue(savedSpending?.name, projectSettings.namePinned)
                ?: WordUtils.capitalizeFully(category.toString().replace("\\_".toRegex(), " ")),
            notes = nonNullAndTrue(savedSpending?.notes),
            type = type,
            value = average,
            fromStartDate = nonNullAndTrue(savedSpending?.fromStartDate, projectSettings.whenPinned)
                ?: firstOccurrence,
            fromEndDate = nonNullAndTrue(savedSpending?.fromEndDate, projectSettings.whenPinned)
                ?: firstOccurrence.add(cycle, cycleMultiplier.toLong()).minusDays(1),
            occurrenceCount = nonNullAndTrue(savedSpending?.occurrenceCount),
            cycleMultiplier = cycleMultiplier,
            cycle = cycle,
            enabled = nonNullAndTrue(savedSpending?.enabled) ?: type.defaultEnabled,
            spent = transactionsGroupedByCycle[cycle.ordinal(LocalDate.now())]?.sumBy { it.amount }?.div(100.0)
                ?: 0.0, // cents to pounds
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
            else -> {
                try {
                    ApiSpending.Category.valueOf(apiCategory.replace("-", "_").toUpperCase())
                } catch (t: Throwable) {
                    ApiSpending.Category.OTHER
                }
            }
        }
    }

    private fun getOptimalCycle(transactions: List<Transaction>): ApiSpending.Cycle {
        var highestDistanceDays = 0L
        val sortedDates = transactions.sortedBy { it.created }.map { it.created.toLocalDate() }
        var distance: Long = 0

        sortedDates.indices.forEach { i ->
            if (i > 0 && { distance = sortedDates[i].toEpochDay() - sortedDates[i - 1].toEpochDay(); distance }() > highestDistanceDays) {
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