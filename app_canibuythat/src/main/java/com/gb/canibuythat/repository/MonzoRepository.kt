package com.gb.canibuythat.repository

import com.gb.canibuythat.MonzoConstants
import com.gb.canibuythat.api.BaseFormDataApi
import com.gb.canibuythat.api.MonzoApi
import com.gb.canibuythat.api.MonzoAuthApi
import com.gb.canibuythat.api.model.ApiTransaction
import com.gb.canibuythat.interactor.ProjectInteractor
import com.gb.canibuythat.model.Login
import com.gb.canibuythat.model.Spending
import com.gb.canibuythat.model.Transaction
import com.gb.canibuythat.model.Webhooks
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
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
                redirectUri = MonzoConstants.MONZO_URI_AUTH_CALLBACK,
                clientId = MonzoConstants.CLIENT_ID,
                clientSecret = MonzoConstants.CLIENT_SECRET)
                .map(mapper::mapToLogin)
    }

    fun getSpendings(accountIds: List<String>): Single<List<Spending>> {
        return Observable.create<ApiTransaction> { emitter ->
            accountIds.forEach { monzoApi.transactions(it).blockingGet().transactions.forEach { emitter.onNext(it) } }
            emitter.onComplete()
        }.map {
            mapper.mapToTransaction(it)
        }.toList().map { transactions ->
            val projectSettings = projectInteractor.getProject().blockingGet()
            val savedSpending = spendingsRepository.all.blockingGet().groupBy { it.sourceData[Spending.SOURCE_MONZO_CATEGORY] }
            transactions.groupBy(Transaction::category).map { (category, transactionsForThatCategory) ->
                mapper.mapToSpending(category, transactionsForThatCategory, savedSpending[category]?.get(0), projectSettings)
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
