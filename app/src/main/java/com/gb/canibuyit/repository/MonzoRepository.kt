package com.gb.canibuyit.repository

import com.gb.canibuyit.CLIENT_ID
import com.gb.canibuyit.CLIENT_SECRET
import com.gb.canibuyit.MONZO_URI_AUTH_CALLBACK
import com.gb.canibuyit.api.BaseFormDataApi
import com.gb.canibuyit.api.MonzoApi
import com.gb.canibuyit.api.MonzoAuthApi
import com.gb.canibuyit.api.model.ApiTransaction
import com.gb.canibuyit.db.model.ApiSpending
import com.gb.canibuyit.interactor.ProjectInteractor
import com.gb.canibuyit.model.Login
import com.gb.canibuyit.model.Spending
import com.gb.canibuyit.model.Transaction
import com.gb.canibuyit.model.Webhooks
import com.gb.canibuyit.util.FORMAT_RFC3339
import com.gb.canibuyit.util.Logger
import io.reactivex.Completable
import io.reactivex.Observable
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
                                          private val mapper: MonzoMapper) : BaseFormDataApi() {

    fun login(authorizationCode: String): Single<Login> {
        return monzoAuthApi.login("authorization_code",
                code = authorizationCode,
                redirectUri = MONZO_URI_AUTH_CALLBACK,
                clientId = CLIENT_ID,
                clientSecret = CLIENT_SECRET)
                .map(mapper::mapToLogin)
    }

    @Suppress("NAME_SHADOWING")
    fun getSpendings(accountIds: List<String>, since: LocalDate? = null): Single<List<Spending>> {
        return Observable.create<ApiTransaction> { emitter ->
            accountIds.forEach {
                val t = monzoApi.transactions(
                        it,
                        since?.let { FORMAT_RFC3339.format(it.atStartOfDay(ZoneId.systemDefault())) }
                ).blockingGet().transactions
                t.forEach { emitter.onNext(it) }
            }
            emitter.onComplete()
        }.filter {
            it.amount != 0
        }.map {
            mapper.mapToTransaction(it)
        }.toList().map { transactions ->
            Logger.d("MonzoRepository", "${transactions.size} transactions loaded")
            val projectSettings = projectInteractor.getProject().blockingGet()
            val savedSpendings = spendingsRepository.all.blockingGet().groupBy { it.sourceData?.get(ApiSpending.SOURCE_MONZO_CATEGORY) }
            transactions.groupBy(Transaction::category).mapNotNull { (category, transactionsForThatCategory) ->
                val savedSpending = savedSpendings[category.toString()]?.get(0)
//                var transactionsForThatCategory = transactionsForThatCategory
//                savedSpending?.let {
//                    transactionsForThatCategory = transactionsForThatCategory.filter { !it.created.isBefore(savedSpending.fromStartDate.toZDT()) }
//                }
                if (transactionsForThatCategory.isEmpty()) {
                    savedSpending?.let {
                        it.value = 0.0
                        it.enabled = false
                        it
                    } ?: let { null }
                } else {
                    savedSpending?.let {
                        if (it.value == 0.0 && !it.enabled) {
                            it.enabled = true
                        }
                    }
                    mapper.mapToSpending(category, transactionsForThatCategory, savedSpending, projectSettings, since)
                }
            }
        }
    }

    fun registerWebhook(accountId: String, url: String): Completable {
        return monzoApi.registerWebhook(accountId, url)
    }

    fun getWebhooks(accountId: String): Single<Webhooks> {
        return monzoApi.getWebhooks(accountId).map(mapper::mapToWebhooks)
    }

    fun deleteWebhook(webhookId: String): Completable {
        return monzoApi.deleteWebhook(webhookId)
    }
}
