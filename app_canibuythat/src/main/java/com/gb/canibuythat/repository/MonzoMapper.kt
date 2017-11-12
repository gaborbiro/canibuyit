package com.gb.canibuythat.repository

import com.gb.canibuythat.api.model.ApiLogin
import com.gb.canibuythat.api.model.ApiTransaction
import com.gb.canibuythat.api.model.ApiWebhook
import com.gb.canibuythat.api.model.ApiWebhooks
import com.gb.canibuythat.interactor.Project
import com.gb.canibuythat.interactor.ProjectInteractor
import com.gb.canibuythat.model.Login
import com.gb.canibuythat.model.Spending
import com.gb.canibuythat.model.Spending.Cycle
import com.gb.canibuythat.model.Transaction
import com.gb.canibuythat.model.Webhook
import com.gb.canibuythat.model.Webhooks
import org.apache.commons.lang3.text.WordUtils
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoField
import org.threeten.bp.temporal.ChronoUnit
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonzoMapper @Inject constructor(private val projectInteractor: ProjectInteractor) {

    fun mapToLogin(apiLogin: ApiLogin): Login {
        val expiresAt = apiLogin.expires_in * 1000 + System.currentTimeMillis()
        return Login(apiLogin.access_token, apiLogin.refresh_token, expiresAt)
    }

    fun mapToTransaction(apiTransaction: ApiTransaction): Transaction {
        val noteCategory = if (!apiTransaction.notes.isEmpty()) Spending.Category.values().firstOrNull { apiTransaction.notes.startsWith(it.toString(), ignoreCase = true) } else null
        val category = mapApiCategory(noteCategory?.toString() ?: apiTransaction.category)
        return Transaction(apiTransaction.amount,
                ZonedDateTime.parse(apiTransaction.created),
                apiTransaction.currency,
                apiTransaction.description,
                apiTransaction.id,
                apiTransaction.notes,
                apiTransaction.is_load,
                if (!apiTransaction.settled.isEmpty()) ZonedDateTime.parse(apiTransaction.settled) else null,
                category)
    }

    fun mapToSpending(category: Spending.Category, transactions: List<Transaction>, savedSpending: Spending?, projectSettings: Project): Spending? {
        val spending = Spending()
        val cycle: Cycle = nonNullAndTrue(savedSpending?.cycle, projectSettings.cyclePinned) ?: getOptimalCycle(transactions)

        spending.cycle = cycle
        val cycleMap: Map<Int, List<Transaction>> = transactions.groupBy { cycle.of(it.created.toLocalDate()) }

        spending.value = nonNullAndTrue(savedSpending?.value, projectSettings.averagePinned) ?: transactions
                .sumBy { it.amount }
                .div(cycle.span(transactions.minBy { it.created }!!.created, ZonedDateTime.now()))
                .toDouble()
        spending.value = nonNullAndTrue(savedSpending?.value, projectSettings.averagePinned) ?: Math.round(spending.value).div(100.0) // cents to pounds
        spending.type = nonNullAndTrue(savedSpending?.type, projectSettings.categoryPinned) ?: category
        spending.name = nonNullAndTrue(savedSpending?.name, projectSettings.namePinned) ?: WordUtils.capitalizeFully(category.toString().replace("\\_".toRegex(), " "))
        spending.cycleMultiplier = nonNullAndTrue(savedSpending?.cycleMultiplier, projectSettings.cyclePinned) ?: 1
        val firstOccurrence: LocalDate = transactions.minBy { it.created }!!.created.toLocalDate()
        spending.fromStartDate = nonNullAndTrue(savedSpending?.fromStartDate, projectSettings.whenPinned) ?: fromLocalDate(firstOccurrence)
        spending.fromEndDate = nonNullAndTrue(savedSpending?.fromEndDate, projectSettings.whenPinned) ?: fromLocalDate(firstOccurrence.add(cycle, 1).minusDays(1))
        spending.sourceData.put(Spending.SOURCE_MONZO_CATEGORY, category.name.toLowerCase())
        cycleMap[cycle.of(LocalDate.now(ZoneId.systemDefault()))]?.let { spending.spent = it.sumBy { it.amount }.div(100.0) } ?: let { spending.spent = 0.0 }

        spending.occurrenceCount = nonNullAndTrue(savedSpending?.occurrenceCount)
        spending.target = nonNullAndTrue(savedSpending?.target)
        spending.notes = nonNullAndTrue(savedSpending?.notes)
        spending.enabled = nonNullAndTrue(savedSpending?.enabled) ?: spending.type!!.defaultEnabled
        return spending
    }

    fun mapToWebhooks(apiWebhooks: ApiWebhooks): Webhooks {
        return Webhooks(apiWebhooks.webhooks.map { mapToWebhook(it) })
    }

    fun mapToWebhook(apiWebhook: ApiWebhook): Webhook {
        return Webhook(apiWebhook.id, apiWebhook.url)
    }

    fun Cycle.of(date: LocalDate): Int {
        return when (this) {
            Cycle.DAYS -> date.toEpochDay().toInt()
            Cycle.WEEKS -> date.minusDays(1)[ChronoField.ALIGNED_WEEK_OF_YEAR]
            Cycle.MONTHS -> date.monthValue
            Cycle.YEARS -> date.year
        }
    }

    fun Cycle.span(start: ZonedDateTime, end: ZonedDateTime): Long {
        return when (this) {
            Cycle.DAYS -> ChronoUnit.DAYS.between(start.withHour(0), end.withHour(24))
            Cycle.WEEKS -> ChronoUnit.WEEKS.between(
                    start.minusDays(start.dayOfWeek.value.toLong() - 1),
                    end.plusDays(7 - end.dayOfWeek.value.toLong()))
            Cycle.MONTHS -> ChronoUnit.MONTHS.between(start.withDayOfMonth(1), end.withDayOfMonth(end.month.length(false)))
            Cycle.YEARS -> ChronoUnit.YEARS.between(start.withDayOfYear(1), end.withDayOfYear(365))
        } + 1
    }

    fun LocalDate.add(cycle: Cycle, amount: Long): LocalDate {
        return when (cycle) {
            Cycle.DAYS -> this.plusDays(amount)
            Cycle.WEEKS -> this.plusWeeks(amount)
            Cycle.MONTHS -> this.plusMonths(amount)
            Cycle.YEARS -> this.plusYears(amount)
        }
    }

    private fun mapApiCategory(apiCategory: String): Spending.Category {
        return when (apiCategory) {
            "mondo" -> Spending.Category.INCOME
            "general" -> Spending.Category.OTHER
            "eating_out" -> Spending.Category.FOOD
            "expenses" -> Spending.Category.EXPENSES
            "transport" -> Spending.Category.TRANSPORTATION
            "cash" -> Spending.Category.CASH
            "bills" -> Spending.Category.UTILITIES
            "entertainment" -> Spending.Category.ENTERTAINMENT
            "shopping" -> Spending.Category.LUXURY
            "holidays" -> Spending.Category.VACATION
            "groceries" -> Spending.Category.GROCERIES
            else -> {
                try {
                    Spending.Category.valueOf(apiCategory.toUpperCase())
                } catch (t: Throwable) {
                    Spending.Category.OTHER
                }
            }
        }
    }

    private fun getOptimalCycle(transactions: List<Transaction>): Spending.Cycle {
        var highestDistanceDays = 0L
        val sortedList = transactions.sortedBy { it.created }.map { it.created.toLocalDate() }
        var distance: Long = 0

        sortedList.indices.forEach { i ->
            if (i > 0 && { distance = sortedList[i].toEpochDay() - sortedList[i - 1].toEpochDay(); distance }() > highestDistanceDays) {
                highestDistanceDays = distance
            }
        }
        return when {
            highestDistanceDays <= 7 -> Spending.Cycle.WEEKS
            highestDistanceDays <= 365 -> Spending.Cycle.MONTHS
            else -> Spending.Cycle.YEARS
        }
    }

    private fun fromLocalDate(localDate: LocalDate): Date = Date(localDate.year - 1900, localDate.monthValue - 1, localDate.dayOfMonth)

    private fun <T> nonNullAndTrue(data: T?, flag: Boolean? = true): T? {
        return if (data != null && flag == true) data else null
    }
}