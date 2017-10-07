package com.gb.canibuythat.repository

import com.gb.canibuythat.api.model.*
import com.gb.canibuythat.model.*
import com.gb.canibuythat.model.Spending.Cycle
import org.apache.commons.lang3.text.WordUtils
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoField
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonzoMapper @Inject constructor() {

    fun mapToLogin(apiLogin: ApiLogin): Login {
        val expiresAt = apiLogin.expires_in * 1000 + System.currentTimeMillis()
        return Login(apiLogin.access_token, apiLogin.refresh_token, expiresAt)
    }

    fun mapToTransactions(apiTransactions: ApiTransactions): List<Transaction> {
        return apiTransactions.transactions.map { mapToTransaction(it) }
    }

    fun mapToTransaction(apiTransaction: ApiTransaction): Transaction {
        return Transaction(apiTransaction.amount,
                ZonedDateTime.parse(apiTransaction.created),
                apiTransaction.currency,
                apiTransaction.description,
                apiTransaction.id,
                apiTransaction.merchant,
                apiTransaction.notes,
                apiTransaction.is_load,
                if (!apiTransaction.settled.isEmpty()) ZonedDateTime.parse(apiTransaction.settled) else null,
                apiTransaction.category)
    }

    fun mapToSpending(category: String, transactions: List<Transaction>): Spending {
        val spending = Spending()
        val cycle = getOptimalCycle(transactions)
        spending.cycle = cycle

        val timeMap: Map<Int, List<Transaction>> = transactions.groupBy { cycle.get(it.created.toLocalDate()) }
        spending.value = transactions.sumBy { it.amount }.div(timeMap.size).div(100.0)
        spending.type = mapSpendingType(category)
        spending.enabled = spending.type!!.defaultEnabled
        spending.name = WordUtils.capitalizeFully(category.replace("\\_".toRegex(), " "))
        spending.occurrenceCount = null
        spending.cycleMultiplier = 1
        val firstOccurrence: LocalDate = transactions.minBy { it.created }!!.created.toLocalDate()
        spending.fromStartDate = fromLocalDate(firstOccurrence)
        spending.fromEndDate = fromLocalDate(firstOccurrence.add(cycle, 1).minusDays(1))
        spending.sourceData.put(Spending.SOURCE_MONZO_CATEGORY, category)
        spending.spent = 0.0
        timeMap[cycle.get(LocalDate.now(ZoneId.systemDefault()))]?.let { spending.spent = it.sumBy { it.amount }.div(100.0) }
        return spending
    }

    fun mapToWebhooks(apiWebhooks: ApiWebhooks): Webhooks {
        return Webhooks(apiWebhooks.webhooks.map { mapToWebhook(it) })
    }

    fun mapToWebhook(apiWebhook: ApiWebhook): Webhook {
        return Webhook(apiWebhook.id, apiWebhook.url)
    }

    fun Cycle.get(date: LocalDate): Int {
        return when (this) {
            Cycle.DAYS -> date.toEpochDay().toInt()
            Cycle.WEEKS -> date.minusDays(1)[ChronoField.ALIGNED_WEEK_OF_YEAR]
            Cycle.MONTHS -> date.monthValue
            Cycle.YEARS -> date.year
        }
    }

    fun LocalDate.add(cycle: Cycle, amount: Long): LocalDate {
        return when (cycle) {
            Cycle.DAYS -> this.plusDays(amount)
            Cycle.WEEKS -> this.plusWeeks(amount)
            Cycle.MONTHS -> this.plusMonths(amount)
            Cycle.YEARS -> this.plusYears(amount)
        }
    }

    fun mapSpendingType(monzoCategory: String): Spending.Category {
        when (monzoCategory) {
            "mondo" -> return Spending.Category.INCOME
            "general" -> return Spending.Category.OTHER
            "eating_out" -> return Spending.Category.FOOD
            "expenses" -> return Spending.Category.EXPENSES
            "transport" -> return Spending.Category.TRANSPORTATION
            "cash" -> return Spending.Category.CASH
            "bills" -> return Spending.Category.UTILITIES
            "entertainment" -> return Spending.Category.ENTERTAINMENT
            "shopping" -> return Spending.Category.LUXURY
            "holidays" -> return Spending.Category.VACATION
            "groceries" -> return Spending.Category.GROCERIES
            else -> return Spending.Category.OTHER
        }
    }

    fun getOptimalCycle(transactions: List<Transaction>): Spending.Cycle {
        var highestDistanceDays = 0L
        val sortedList = transactions.sortedBy { it.created }.map { it.created.toLocalDate() }
        var distance: Long = 0

        for (i in sortedList.indices) {
            if (i > 0 && { distance = sortedList[i].toEpochDay() - sortedList[i - 1].toEpochDay(); distance }() > highestDistanceDays) {
                highestDistanceDays = distance
            }
        }
        return if (highestDistanceDays <= 7) Spending.Cycle.WEEKS else
            if (highestDistanceDays <= 365) Spending.Cycle.MONTHS else
                Spending.Cycle.YEARS
    }

    fun fromLocalDate(localDate: LocalDate): Date = Date(localDate.year - 1900, localDate.monthValue - 1, localDate.dayOfMonth)
}
