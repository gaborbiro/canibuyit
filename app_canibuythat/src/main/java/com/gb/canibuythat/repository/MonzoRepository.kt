package com.gb.canibuythat.repository

import android.text.TextUtils
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
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.threeten.bp.temporal.ChronoUnit
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
            TextUtils.isEmpty(it.decline_reason) && !TextUtils.isEmpty(it.created)
        }.map {
            mapper.mapToTransaction(it)
        }.toList().map { transactions ->
            val projectSettings = projectInteractor.getProject().blockingGet()
            val savedSpending = spendingsRepository.all.blockingGet()
                    .groupBy { it.sourceData[Spending.SOURCE_MONZO_CATEGORY] }
            val dayCount = ChronoUnit.DAYS.between(transactions.minBy { it.created }!!.created, transactions.maxBy { it.created }!!.created).toInt() + 1
            transactions.groupBy(Transaction::category).map { (category, transactionsForThatCategory) ->
                mapper.mapToSpending(category, transactionsForThatCategory, savedSpending[category]?.get(0), projectSettings, dayCount)
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
