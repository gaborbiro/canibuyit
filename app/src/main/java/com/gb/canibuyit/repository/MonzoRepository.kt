package com.gb.canibuyit.repository

import com.gb.canibuyit.CLIENT_ID
import com.gb.canibuyit.CLIENT_SECRET
import com.gb.canibuyit.MONZO_URI_AUTH_CALLBACK
import com.gb.canibuyit.api.BaseFormDataApi
import com.gb.canibuyit.api.MonzoApi
import com.gb.canibuyit.api.MonzoAuthApi
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
        val sinceStr = since?.let { FORMAT_RFC3339.format(it.atStartOfDay(ZoneId.systemDefault())) }

        return Observable.just(accountIds) // -> Observable<List<String>>
            .flatMapIterable { x -> x } // -> Observable<String>
            .map { accountId ->
                monzoApi.transactions(accountId = accountId, since = sinceStr) // -> Single<ApiTransactions>
                    .map { apiTransactions -> apiTransactions.transactions.toList() } // -> Single<List<ApiTransaction>>
            } // -> Observable<Single<Array<ApiTransaction>>>
            .flatMapSingle { x -> x } // -> Observable<List<ApiTransaction>>
            .flatMapIterable { x -> x } // -> Observable<ApiTransaction>
            .filter { it.amount != 0 && it.decline_reason.isNullOrEmpty() }
            .map { mapper.mapToTransaction(it) } // -> Observable<Transaction>
            .toList() // -> Single<MutableList<Transaction>>
            .map { transactions ->
                Logger.d("MonzoRepository", "${transactions.size} valid transactions loaded")

                val map = transactions
                    .groupBy(Transaction::category)
                return@map map.mapNotNull { (category, transactionsForThatCategory) ->
                    val projectSettings = projectInteractor.getProject().blockingGet()
                    val savedSpendings = spendingsRepository.all.blockingGet().groupBy { it.sourceData?.get(ApiSpending.SOURCE_MONZO_CATEGORY) }

                    val savedSpending = savedSpendings[category.toString()]?.get(0)
                    return@mapNotNull mapper.mapToSpending(category, transactionsForThatCategory, savedSpending, projectSettings, since)
                }
            }
    }

    fun registerWebHook(accountId: String, url: String): Completable {
        return monzoApi.registerWebHook(accountId, url)
    }

    fun getWebHooks(accountId: String): Single<Webhooks> {
        return monzoApi.getWebHooks(accountId).map(mapper::mapToWebhooks)
    }

    fun deleteWebHook(webHookId: String): Completable {
        return monzoApi.deleteWebHook(webHookId)
    }
}
