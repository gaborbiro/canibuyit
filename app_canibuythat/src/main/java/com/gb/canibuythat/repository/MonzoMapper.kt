package com.gb.canibuythat.repository

import com.gb.canibuythat.api.model.*
import com.gb.canibuythat.model.Account
import com.gb.canibuythat.model.BudgetItem
import com.gb.canibuythat.model.Login
import com.gb.canibuythat.model.Transaction
import org.apache.commons.lang3.text.WordUtils
import org.threeten.bp.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonzoMapper @Inject constructor() {

    fun map(apiLogin: ApiLogin): Login {
        return Login(apiLogin.access_token, apiLogin.refresh_token)
    }

    fun map(apiAccountCollection: ApiAccountCollection): List<Account> {
        return apiAccountCollection.accounts.map { map(it) }
    }

    fun map(apiAccount: ApiAccount): Account {
        return Account(apiAccount.id,
                apiAccount.created,
                apiAccount.description)
    }

    fun map(apiTransactionCollection: ApiTransactionCollection): List<Transaction> {
        return apiTransactionCollection.transactions.map { map(it) }
    }

    fun map(apiTransaction: ApiTransaction): Transaction {
        return Transaction(apiTransaction.amount / 100.0,
                ZonedDateTime.parse(apiTransaction.created),
                apiTransaction.currency,
                apiTransaction.description,
                apiTransaction.id,
                apiTransaction.merchant,
                apiTransaction.notes,
                apiTransaction.is_load,
                if (!apiTransaction.settled.isEmpty()) ZonedDateTime.parse(apiTransaction.settled) else null,
                getModelCategory(apiTransaction.category))
    }

    fun mapCategory(monzoCategory: String): BudgetItem.BudgetItemType {
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

    private fun getModelCategory(apiCategory: String): String {
        val noUnderscore = apiCategory.replace("\\_".toRegex(), " ")
        return WordUtils.capitalizeFully(noUnderscore)
    }
}
