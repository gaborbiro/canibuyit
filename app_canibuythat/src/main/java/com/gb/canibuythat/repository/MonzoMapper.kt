package com.gb.canibuythat.repository

import com.gb.canibuythat.api.model.*
import com.gb.canibuythat.model.Account
import com.gb.canibuythat.model.BudgetItem
import com.gb.canibuythat.model.Login
import com.gb.canibuythat.model.Transaction
import org.apache.commons.lang3.text.WordUtils
import org.apache.commons.lang3.time.DateUtils
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoField
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonzoMapper @Inject constructor() {

    fun mapToLogin(apiLogin: ApiLogin): Login {
        return Login(apiLogin.access_token, apiLogin.refresh_token)
    }

    fun mapToAccounts(apiAccountCollection: ApiAccountCollection): List<Account> {
        return apiAccountCollection.accounts.map { mapToAccount(it) }
    }

    fun mapToAccount(apiAccount: ApiAccount): Account {
        return Account(apiAccount.id,
                apiAccount.created,
                apiAccount.description)
    }

    fun mapToTransactions(apiTransactionCollection: ApiTransactionCollection): List<Transaction> {
        return apiTransactionCollection.transactions.map { mapToTransaction(it) }
    }

    fun mapToTransaction(apiTransaction: ApiTransaction): Transaction {
        return Transaction(apiTransaction.amount,
                LocalDateTime.parse(apiTransaction.created),
                apiTransaction.currency,
                apiTransaction.description,
                apiTransaction.id,
                apiTransaction.merchant,
                apiTransaction.notes,
                apiTransaction.is_load,
                if (!apiTransaction.settled.isEmpty()) ZonedDateTime.parse(apiTransaction.settled) else null,
                apiTransaction.category)
    }

    fun mapToBudgetItem(category: String, transactions: List<Transaction>): BudgetItem {
        val budgetItem = BudgetItem()
        budgetItem.periodType = getPeriodType(transactions)

        fun period(periodType: BudgetItem.PeriodType): Int {
            return when (periodType) {
                BudgetItem.PeriodType.DAYS -> LocalDate.now().toEpochDay().toInt()
                BudgetItem.PeriodType.WEEKS -> LocalDate.now()[ChronoField.ALIGNED_WEEK_OF_YEAR]
                BudgetItem.PeriodType.MONTHS -> LocalDate.now().monthValue
                BudgetItem.PeriodType.YEARS -> LocalDate.now().year
            }
        }

        val monthMap: Map<Int, List<Transaction>> = transactions.groupBy { period(budgetItem.periodType!!) }
        budgetItem.amount = transactions.sumBy { it.amount }.div(monthMap.size).div(100.0)
        budgetItem.type = mapBudgetItemType(category)
        budgetItem.enabled = budgetItem.type!!.defaultEnabled
        budgetItem.name = WordUtils.capitalizeFully(category.replace("\\_".toRegex(), " "))
        budgetItem.occurrenceCount = null
        budgetItem.periodMultiplier = 1
        val firstOccurrence: LocalDateTime = transactions.minBy { it.created }!!.created
        budgetItem.firstOccurrenceStart = fromLocalDate(firstOccurrence)
        budgetItem.firstOccurrenceEnd = fromLocalDate(firstOccurrence.plusMonths(1))
        budgetItem.sourceData.put(BudgetItem.SOURCE_MONZO_CATEGORY, category)
        monthMap[period(budgetItem.periodType!!)]?.let { budgetItem.spent = it.sumBy { it.amount }.div(100.0) }
        return budgetItem
    }

    fun mapBudgetItemType(monzoCategory: String): BudgetItem.BudgetItemType {
        when (monzoCategory) {
            "mondo" -> return BudgetItem.BudgetItemType.INCOME
            "general" -> return BudgetItem.BudgetItemType.OTHER
            "eating_out" -> return BudgetItem.BudgetItemType.FOOD
            "expenses" -> return BudgetItem.BudgetItemType.EXPENSES
            "transport" -> return BudgetItem.BudgetItemType.TRANSPORTATION
            "cash" -> return BudgetItem.BudgetItemType.CASH
            "bills" -> return BudgetItem.BudgetItemType.UTILITIES
            "entertainment" -> return BudgetItem.BudgetItemType.ENTERTAINMENT
            "shopping" -> return BudgetItem.BudgetItemType.LUXURY
            "holidays" -> return BudgetItem.BudgetItemType.VACATION
            "groceries" -> return BudgetItem.BudgetItemType.GROCERIES
            else -> return BudgetItem.BudgetItemType.OTHER
        }
    }

    fun getPeriodType(transactions: List<Transaction>): BudgetItem.PeriodType {
        var highestDistanceSeconds = 0L
        val sortedList = transactions.sortedBy { it.created }
        var distance: Long = 0

        for (i in sortedList.indices) {
            if (i > 0 && { distance = sortedList[i].created.toEpochSecond(ZoneOffset.ofTotalSeconds(0)) - sortedList[i - 1].created.toEpochSecond(ZoneOffset.ofTotalSeconds(0)); distance }() > highestDistanceSeconds) {
                highestDistanceSeconds = distance
            }
        }
        return if (highestDistanceSeconds <= DateUtils.MILLIS_PER_DAY * 7 / 1000) BudgetItem.PeriodType.WEEKS else
            if (highestDistanceSeconds <= DateUtils.MILLIS_PER_DAY * 365 / 1000) BudgetItem.PeriodType.MONTHS else
                BudgetItem.PeriodType.YEARS
    }

    fun fromLocalDate(localDateTime: LocalDateTime): Date = Date(localDateTime.year, localDateTime.monthValue, localDateTime.dayOfMonth)
}
