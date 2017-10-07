package com.gb.canibuythat.repository

import com.gb.canibuythat.MonzoConstants
import com.gb.canibuythat.api.BaseFormDataApi
import com.gb.canibuythat.api.MonzoApi
import com.gb.canibuythat.api.MonzoAuthApi
import com.gb.canibuythat.api.model.ApiTransaction
import com.gb.canibuythat.api.model.ApiTransactions
import com.gb.canibuythat.model.Login
import com.gb.canibuythat.model.Spending
import com.gb.canibuythat.model.Webhooks
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonzoRepository @Inject constructor(private val monzoApi: MonzoApi,
                                          private val monzoAuthApi: MonzoAuthApi,
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
        return Observable.create<ApiTransactions> { emitter ->
            accountIds.forEach { emitter.onNext(monzoApi.transactions(it).blockingGet()) }
            emitter.onComplete()
        }.collectInto(mutableListOf<ApiTransaction>()) { collector, apiTransactions ->
            collector.addAll(apiTransactions.transactions)
        }
                .map {
                    mapper.mapToTransactions(ApiTransactions(it.toTypedArray()))
                }
                .map { transactions ->
                    transactions.groupBy { transaction -> transaction.category }.map { (category, transactionsForCategory) ->
                        mapper.mapToSpending(category, transactionsForCategory)
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
