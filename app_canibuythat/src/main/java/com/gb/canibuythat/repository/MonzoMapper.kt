package com.gb.canibuythat.repository

import com.gb.canibuythat.api.model.*
import com.gb.canibuythat.model.Account
import com.gb.canibuythat.model.BudgetItem
import com.gb.canibuythat.model.Login
import com.gb.canibuythat.model.Transaction
import org.apache.commons.lang3.text.WordUtils
import org.apache.commons.lang3.time.DateUtils
import org.threeten.bp.ZonedDateTime
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

    fun mapToBudgetItem(category: String, transactions: List<Transaction>): BudgetItem {
        val budgetItem = BudgetItem()
        budgetItem.amount = transactions.sumBy { it.amount } / transactions.groupBy { it.created.month }.size / 100.0
        budgetItem.type = mapBudgetItemType(category)
        budgetItem.enabled = budgetItem.type!!.defaultEnabled
        budgetItem.name = WordUtils.capitalizeFully(category.replace("\\_".toRegex(), " "))
        budgetItem.occurrenceCount = null
        budgetItem.periodMultiplier = 1
        budgetItem.periodType = getPeriodType(transactions)
        val firstOccurrence: ZonedDateTime = transactions.minBy { it.created }!!.created
        budgetItem.firstOccurrenceStart = Date(firstOccurrence.toEpochSecond() * 1000)
        budgetItem.firstOccurrenceEnd = Date(firstOccurrence.plusMonths(1).toEpochSecond() * 1000)
        budgetItem.sourceData.put(BudgetItem.SOURCE_MONZO_CATEGORY, category)
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
            if (i > 0 && { distance = sortedList[i].created.toEpochSecond() - sortedList[i - 1].created.toEpochSecond(); distance }() > highestDistanceSeconds) {
                highestDistanceSeconds = distance
            }
        }
        return if (highestDistanceSeconds <= DateUtils.MILLIS_PER_DAY * 7 / 1000) BudgetItem.PeriodType.WEEKS else
            if (highestDistanceSeconds <= DateUtils.MILLIS_PER_DAY * 365 / 1000) BudgetItem.PeriodType.MONTHS else
                BudgetItem.PeriodType.YEARS
    }
}
