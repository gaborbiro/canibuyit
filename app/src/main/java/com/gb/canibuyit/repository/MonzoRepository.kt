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
import com.gb.canibuyit.model.SerializableMap
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
                    val startDate = since ?: transactions[0].created
                    val endDate = LocalDate.now()
                    Logger.d("MonzoRepository", "Processing ${transactions.size} transactions, from $startDate to $endDate")
                    return@map transactions.groupBy(Transaction::category)
                            .mapNotNull { (category, transactionsForThatCategory) ->
                                Logger.d("MonzoRepository", "${transactionsForThatCategory.size} $category")
                                return@mapNotNull getSpending(category, transactionsForThatCategory, startDate, endDate)
                            }
                }
    }

    private fun getSpending(category: ApiSpending.Category, transactionsForThatCategory: List<Transaction>, startDate: LocalDate, endDate: LocalDate): Spending {
        val projectSettings = projectInteractor.getProject().blockingGet()
        val savedSpendings = spendingsRepository.all.blockingGet()
                .groupBy { it.sourceData?.get(ApiSpending.SOURCE_MONZO_CATEGORY) }

        val savedSpending = savedSpendings[category.toString()]?.get(0)
        val size = transactionsForThatCategory.size
        return if (size > 1) {
            val firstHalf = transactionsForThatCategory.subList(0, size / 2)
            val secondHalf = transactionsForThatCategory.subList(size / 2, size)
            val spending1 = mapper.mapToSpending(
                    category,
                    firstHalf,
                    savedSpending,
                    projectSettings,
                    startDate,
                    secondHalf.first().created)
            val spending2 = mapper.mapToSpending(
                    category,
                    secondHalf,
                    savedSpending,
                    projectSettings,
                    secondHalf.first().created,
                    endDate)
            Spending(
                    id = spending1.id,
                    targets = spending1.targets,
                    name = spending1.name,
                    notes = spending1.notes,
                    type = spending1.type,
                    value = (spending1.value + spending2.value) / 2,
                    fromStartDate = least(spending1.fromStartDate, spending2.fromStartDate),
                    fromEndDate = most(spending1.fromEndDate, spending2.fromEndDate),
                    occurrenceCount = savedSpending?.occurrenceCount,
                    cycleMultiplier = spending1.cycleMultiplier,
                    cycle = most(spending1.cycle, spending2.cycle),
                    enabled = spending1.enabled,
                    spent = spending1.spent + spending2.spent,
                    savings = spending1.savings + spending2.savings,
                    sourceData = SerializableMap<String, String>().apply { put(ApiSpending.SOURCE_MONZO_CATEGORY, category.name.toLowerCase()) })
        } else {
            mapper.mapToSpending(
                    category,
                    transactionsForThatCategory,
                    savedSpending,
                    projectSettings,
                    startDate,
                    endDate)
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

fun <T : Comparable<T>> least(a: T, b: T) = if (a < b) a else b
fun <T : Comparable<T>> most(a: T, b: T) = if (a > b) a else b

operator fun <T> Array<T>?.plus(array: Array<T>?): Array<T>? {
    return when {
        this == null -> array
        array == null -> this
        else -> {
            val thisSize = size
            val arraySize = array.size
            val result = java.util.Arrays.copyOf(this, thisSize + arraySize)
            System.arraycopy(array, 0, result, thisSize, arraySize)
            result
        }
    }
}