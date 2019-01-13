package com.gb.canibuyit.repository

import com.gb.canibuyit.CLIENT_ID
import com.gb.canibuyit.CLIENT_SECRET
import com.gb.canibuyit.MONZO_URI_AUTH_CALLBACK
import com.gb.canibuyit.api.BaseFormDataApi
import com.gb.canibuyit.api.MonzoApi
import com.gb.canibuyit.api.MonzoAuthApi
import com.gb.canibuyit.db.model.ApiSpending
import com.gb.canibuyit.interactor.ProjectInteractor
import com.gb.canibuyit.model.CycleSpent
import com.gb.canibuyit.model.Login
import com.gb.canibuyit.model.Spending
import com.gb.canibuyit.model.Transaction
import com.gb.canibuyit.model.Webhooks
import com.gb.canibuyit.model.copy
import com.gb.canibuyit.util.FORMAT_RFC3339
import com.gb.canibuyit.util.Logger
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.math.RoundingMode
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
                                return@mapNotNull convertTransactionsToSpending(category, transactionsForThatCategory, startDate, endDate)
                            }
                }
    }

    private fun convertTransactionsToSpending(
        category: ApiSpending.Category,
        transactionsByCategory: List<Transaction>,
        startDate: LocalDate,
        endDate: LocalDate): Spending {
        val projectSettings = projectInteractor.getProject().blockingGet()
        val savedSpendings = spendingsRepository.getAll().blockingGet()
                .groupBy { it.sourceData?.get(ApiSpending.SOURCE_MONZO_CATEGORY) }
        val savedSpending = savedSpendings[category.toString()]?.get(0)
        val disabledCycles = savedSpending?.spentByCycle?.filter { !it.enabled }.orEmpty()

        if (disabledCycles.isNotEmpty()) {
            val cycleCount = savedSpending!!.spentByCycle!!.count(CycleSpent::enabled)
            val filteredTransactions: Array<List<Transaction>> = excludeRanges(
                    list = transactionsByCategory,
                    ranges = disabledCycles.map { Pair(it.from, it.to) },
                    compare = { transaction, date -> transaction.created.compareTo(date) })
            val inclusiveRanges: List<Pair<LocalDate, LocalDate>> = disabledCycles.map { arrayOf(it.from.minusDays(1), it.to.plusDays(1)) }
                    .toTypedArray().flatten().toMutableList().apply {
                        add(0, startDate)
                        add(endDate)
                    }.run {
                        filterIndexed { index, _ -> index % 2 == 0 } zip filterIndexed { index, _ -> index % 2 != 0 }
                    }
            return (inclusiveRanges zip filteredTransactions).filter { it.second.isNotEmpty() }.map { (range, transactions) ->
                val (rangeStart, rangeEnd) = range
                mapper.mapToSpending(
                        category,
                        transactions,
                        savedSpending,
                        projectSettings,
                        rangeStart,
                        rangeEnd)
            }.foldIndexed(null) { index, accumulator: Spending?, next ->
                val result = accumulator?.merge(
                        next,
                        isCurrentCycle = index == cycleCount - 1) ?: next
                result
            }.let {
                it?.copy(value = it.total.divide(cycleCount.toBigDecimal(), RoundingMode.DOWN).divide(100.toBigDecimal()))
            }!!
        } else {
            return mapper.mapToSpending(
                    category,
                    transactionsByCategory,
                    savedSpending,
                    projectSettings,
                    startDate,
                    endDate)
        }
    }

    private fun <T, D> excludeRanges(list: List<T>,
                                     ranges: List<Pair<D, D>>,
                                     compare: (a: T, b: D) -> Int): Array<List<T>> {
        infix fun T.isBefore(d: D) = compare(this, d) < 0
        infix fun T.isAfter(d: D) = compare(this, d) > 0
        val result: Array<MutableList<T>> = Array(ranges.size + 1) { _ -> mutableListOf<T>() }
        var listIndex = 0
        var rangesIndex = 0

        while (listIndex < list.size) {
            if (rangesIndex >= ranges.size || list[listIndex] isBefore ranges[rangesIndex].first) {
                result[rangesIndex].add(list[listIndex])
            }
            listIndex++
            if (rangesIndex < ranges.size && list[listIndex] isAfter ranges[rangesIndex].second) {
                rangesIndex++
            }
        }
        return result as Array<List<T>>
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