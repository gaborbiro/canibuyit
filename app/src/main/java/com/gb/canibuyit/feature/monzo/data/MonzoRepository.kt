package com.gb.canibuyit.feature.monzo.data

import com.gb.canibuyit.feature.monzo.CLIENT_ID
import com.gb.canibuyit.feature.monzo.CLIENT_SECRET
import com.gb.canibuyit.feature.monzo.MONZO_CATEGORY
import com.gb.canibuyit.feature.monzo.MONZO_URI_AUTH_CALLBACK
import com.gb.canibuyit.feature.monzo.api.MonzoApi
import com.gb.canibuyit.feature.monzo.api.MonzoAuthApi
import com.gb.canibuyit.feature.monzo.api.model.ApiMonzoTransactions
import com.gb.canibuyit.feature.monzo.model.Login
import com.gb.canibuyit.feature.monzo.model.Transaction
import com.gb.canibuyit.feature.monzo.model.Webhooks
import com.gb.canibuyit.feature.project.data.ProjectInteractor
import com.gb.canibuyit.feature.spending.data.SpendingsRepository
import com.gb.canibuyit.feature.spending.model.Spending
import com.gb.canibuyit.feature.spending.persistence.model.ApiSpending
import com.gb.canibuyit.util.FORMAT_RFC3339
import com.gb.canibuyit.util.Logger
import io.reactivex.Completable
import io.reactivex.Single
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonzoRepository @Inject constructor(private val monzoApi: MonzoApi,
                                          private val monzoAuthApi: MonzoAuthApi,
                                          private val projectInteractor: ProjectInteractor,
                                          private val spendingsRepository: SpendingsRepository,
                                          private val mapper: MonzoMapper) {

    fun login(authorizationCode: String): Single<Login> {
        return monzoAuthApi.login("authorization_code",
                code = authorizationCode,
                redirectUri = MONZO_URI_AUTH_CALLBACK,
                clientId = CLIENT_ID,
                clientSecret = CLIENT_SECRET)
                .map(mapper::mapToLogin)
    }

    fun getSpendings(accountId: String, since: LocalDate? = null,
                     before: LocalDate): Single<List<Spending>> {
        return getRawTransactions(accountId, since, before)
                .map { transactions: List<Transaction> ->
                    val startDate = since ?: transactions[0].created
                    Logger.d("MonzoRepository",
                            "Processing ${transactions.size} transactions, from $startDate to $before")
                    transactions.groupBy(Transaction::category)
                            .mapNotNull { (category, transactionsForThatCategory) ->
                                Logger.d("MonzoRepository",
                                        "${transactionsForThatCategory.size} $category")
                                return@mapNotNull convertTransactionsToSpending(category,
                                        transactionsForThatCategory, startDate, before)
                            }
                }
    }

    fun getRawTransactions(accountId: String, since: LocalDate? = null,
                           before: LocalDate): Single<List<Transaction>> {
        val sinceStr = since?.let { FORMAT_RFC3339.format(it.atStartOfDay(ZoneId.systemDefault())) }
        val beforeStr = FORMAT_RFC3339.format(
                before.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1))

        return monzoApi.transactions(accountId = accountId, since = sinceStr, before = beforeStr)
                .map { apiMonzoTransactions: ApiMonzoTransactions ->
                    apiMonzoTransactions.transactions
                            .filter { apiTransaction ->
                                apiTransaction.amount != 0 &&
                                        apiTransaction.decline_reason.isNullOrEmpty()
                            }
                            .map(mapper::mapApiTransaction)
                }
    }

    private fun convertTransactionsToSpending(category: ApiSpending.Category,
                                              transactionsByCategory: List<Transaction>,
                                              startDate: LocalDate,
                                              endDate: LocalDate): Spending {
        val projectSettings = projectInteractor.getProject().blockingGet()
        val savedSpendings = spendingsRepository.getAll().blockingGet()
                .groupBy { it.sourceData?.get(MONZO_CATEGORY) }
        val savedSpending = savedSpendings[category.toString()]?.get(0)
        return mapper.mapToSpending(
                category,
                transactionsByCategory,
                savedSpending,
                projectSettings,
                startDate,
                endDate)
    }

    fun registerWebHook(accountId: String, url: String): Completable {
        return monzoApi.registerWebhook(accountId, url)
    }

    fun getWebHooks(accountId: String): Single<Webhooks> {
        return monzoApi.getWebhooks(accountId).map(mapper::mapToWebhooks)
    }

    fun deleteWebHook(webHookId: String): Completable {
        return monzoApi.deleteWebhook(webHookId)
    }
}

fun <T : Comparable<T>> least(a: T, b: T) = if (a < b) a else b