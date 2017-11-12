package com.gb.canibuythat.repository

import com.gb.canibuythat.CLIENT_ID
import com.gb.canibuythat.CLIENT_SECRET
import com.gb.canibuythat.MONZO_URI_AUTH_CALLBACK
import com.gb.canibuythat.api.BaseFormDataApi
import com.gb.canibuythat.api.MonzoApi
import com.gb.canibuythat.api.MonzoAuthApi
import com.gb.canibuythat.api.model.ApiTransaction
import com.gb.canibuythat.interactor.ProjectInteractor
import com.gb.canibuythat.model.Login
import com.gb.canibuythat.model.Spending
import com.gb.canibuythat.model.Transaction
import com.gb.canibuythat.model.Webhooks
import com.gb.canibuythat.util.DateUtils
import com.gb.canibuythat.util.toZDT
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.util.*
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
    fun getSpendings(accountIds: List<String>, since: Date? = null): Single<List<Spending>> {
        return Observable.create<ApiTransaction> { emitter ->
            accountIds.forEach {
                monzoApi.transactions(
                        it,
                        since?.let { DateUtils.FORMAT_RFC3339.format(it) }
                ).blockingGet().transactions.forEach { emitter.onNext(it) }
            }
            emitter.onComplete()
        }.distinct {
            it.id
        }.filter {
            it.include_in_spending
        }.map {
            mapper.mapToTransaction(it)
        }.toList().map { transactions ->
            val projectSettings = projectInteractor.getProject().blockingGet()
            val savedSpendings = spendingsRepository.all.blockingGet().groupBy { it.sourceData[Spending.SOURCE_MONZO_CATEGORY] }
            transactions.groupBy(Transaction::category).mapNotNull { (category, transactionsForThatCategory) ->
                val savedSpending = savedSpendings[category.toString()]?.get(0)
                var transactionsForThatCategory = transactionsForThatCategory
                savedSpending?.let {
                    transactionsForThatCategory = transactionsForThatCategory.filter { !it.created.isBefore(savedSpending.fromStartDate.toZDT()) }
                }
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
                    mapper.mapToSpending(category, transactionsForThatCategory, savedSpending, projectSettings)
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
