package com.gb.canibuythat.repository

import android.text.TextUtils
import com.gb.canibuythat.UserPreferences
import com.gb.canibuythat.db.Contract
import com.gb.canibuythat.db.SpendingDBHelper
import com.gb.canibuythat.exception.DomainException
import com.gb.canibuythat.interactor.ProjectInteractor
import com.gb.canibuythat.model.Balance
import com.gb.canibuythat.model.Spending
import com.j256.ormlite.dao.Dao
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import java.sql.SQLException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpendingsRepository @Inject
constructor(spendingDBHelper: SpendingDBHelper,
            private val projectInteractor: ProjectInteractor,
            private val userPreferences: UserPreferences) {
    private val spendingDao: Dao<Spending, Int> = spendingDBHelper.getDao<Dao<Spending, Int>, Spending>(Spending::class.java)

    val all: Single<List<Spending>>
        get() {
            return Single.create<List<Spending>> { emitter ->
                try {
                    emitter.onSuccess(spendingDao.queryForAll())
                } catch (e: SQLException) {
                    emitter.onError(e)
                }
            }
        }

    fun createOrUpdate(spending: Spending): Single<Dao.CreateOrUpdateStatus> {
        return Single.create { emitter ->
            try {
                emitter.onSuccess(spendingDao.createOrUpdate(spending))
            } catch (e: SQLException) {
                emitter.onError(e)
            }
        }
    }

    /**
     * Every spending has a local category and a monzo category.
     *
     * For each spending:
     *
     *     - if it does not yet exist in the databse, create it
     *     - if it does exist, update its name, value, cycle start and end date, enabled and spent
     *
     * @param spendings from Monzo
     */
    fun createOrUpdateMonzoSpendings(spendings: List<Spending>): Completable {
        return Completable.create { emitter ->
            val projectSettings = projectInteractor.getProject().blockingGet()

            val savedSpendings: List<Spending> = spendingDao.queryForAll()
                    .filter { it.sourceData[Spending.SOURCE_MONZO_CATEGORY] != null }
                    .sortedBy { it.sourceData[Spending.SOURCE_MONZO_CATEGORY] }

            try {
                spendings.forEach {
                    val category: String? = it.sourceData[Spending.SOURCE_MONZO_CATEGORY]

                    if (category == null) {
                        emitter.onError(Exception("Spending '" + it.name + "' has no category"))
                    } else {
                        val index = savedSpendings.indexOfFirst { it.sourceData[Spending.SOURCE_MONZO_CATEGORY] == category }

                        if (index >= 0) {
                            it.id = savedSpendings[index].id

                            it.notes = savedSpendings[index].notes
                            it.target = savedSpendings[index].target
                            if (projectSettings.cycleOverride) {
                                it.cycle = savedSpendings[index].cycle
                                it.cycleMultiplier = savedSpendings[index].cycleMultiplier
                            }
                            if (projectSettings.categoryOverride) {
                                it.type = savedSpendings[index].type
                            }
                            it.occurrenceCount = savedSpendings[index].occurrenceCount
                            it.enabled = savedSpendings[index].enabled

                            if (projectSettings.averageOverride) {
                                it.value = savedSpendings[index].value
                            }
                            spendingDao.update(it)
                        } else {
                            spendingDao.create(it)
                        }
                    }
                }
                emitter.onComplete()
            } catch (e: SQLException) {
                emitter.onError(Exception("Error creating/updating monzo categories", e))
            }
        }
    }

    fun delete(id: Int): Completable {
        return Completable.create { emitter ->
            try {
                if (spendingDao.deleteById(id) > 0) {
                    emitter.onComplete()
                } else {
                    emitter.onError(Exception("Delete error: spending $id was not found in the database"))
                }
            } catch (e: SQLException) {
                emitter.onError(e)
            }
        }
    }

    fun deleteAll(): Completable {
        return Completable.create { emitter ->
            try {
                if (spendingDao.delete(spendingDao.deleteBuilder().prepare()) > 0) {
                    emitter.onComplete()
                } else {
                    emitter.onError(Exception("Delete error: couldn't clear table"))
                }
            } catch (e: SQLException) {
                emitter.onError(e)
            }
        }
    }

    fun read(id: Int): Maybe<Spending> {
        return Maybe.create<Spending> { emitter ->
            try {
                val spending = spendingDao.queryForId(id)

                if (spending != null) {
                    emitter.onSuccess(spending)
                } else {
                    emitter.onComplete()
                }
            } catch (e: SQLException) {
                emitter.onError(e)
            }
        }
    }

    fun getBalance(): Single<Balance> {
        try {
            val balance = calculateBalanceForCategory(null, startDate = userPreferences.balanceReading?.`when`, endDate = userPreferences.estimateDate)
            balance?.let {
                userPreferences.balanceReading?.let {
                    balance.definitely = balance.definitely?.plus(it.balance)
                    balance.maybeEvenThisMuch = balance.maybeEvenThisMuch?.plus(it.balance)
                }
                return Single.just(balance)
            }
            return Single.just(Balance())
        } catch (e: IllegalArgumentException) {
            return Single.error(DomainException("Date of balance reading must not come after date of target estimate", e))
        }
    }

    fun getCategoryBalance(): String {
        val buffer = StringBuffer()
        val startDate = userPreferences.balanceReading?.`when`
        val endDate = userPreferences.estimateDate

        Spending.Category.values().forEach { category ->
            try {
                val balance = calculateBalanceForCategory(category, startDate, endDate)
                if (balance != null) {
                    buffer.append("${category.name}:\n${balance.definitely}/${balance.maybeEvenThisMuch}\n")
                }
            } catch (e: IllegalArgumentException) {
                throw DomainException("Date of balance reading must not come after date of target estimate", e)
            }
        }
        return buffer.toString()
    }

    /**
     * @param category for which the balance should be calculated. If null, all categories will be included.
     * @param startDate from which the calculation should start. If null, the individual spending start-dates will be used.
     * @param endDate up until which the calculations should go. If null, `today` is used.
     */
    private fun calculateBalanceForCategory(category: Spending.Category?, startDate: Date?, endDate: Date): Balance? {
        var balance: Balance? = null

        val query: MutableMap<String, Any> = mutableMapOf(Contract.Spending.ENABLED to true)
        category?.let { query.put(Contract.Spending.TYPE, category) }
        // blocking thread


        for (spending: Spending in spendingDao.queryForFieldValues(query)) {
            if (balance == null) {
                balance = Balance(0f, 0f)
            }
            val (definitely, maybeEvenThisMuch, _) = BalanceCalculator.getEstimatedBalance(
                    spending, startDate, endDate)
            balance.definitely = balance.definitely?.plus(definitely)
            balance.maybeEvenThisMuch = balance.maybeEvenThisMuch?.plus(maybeEvenThisMuch)
        }

        return balance
    }
}
