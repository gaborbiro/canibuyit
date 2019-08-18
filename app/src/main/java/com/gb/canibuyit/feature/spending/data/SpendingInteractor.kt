package com.gb.canibuyit.feature.spending.data

import android.annotation.SuppressLint
import com.gb.canibuyit.base.model.Lce
import com.gb.canibuyit.base.rx.SchedulerProvider
import com.gb.canibuyit.error.DomainException
import com.gb.canibuyit.feature.monzo.MONZO_CATEGORY
import com.gb.canibuyit.feature.monzo.data.MonzoInteractor
import com.gb.canibuyit.feature.spending.model.Spending
import com.gb.canibuyit.feature.spending.persistence.model.ApiSpending
import com.gb.canibuyit.feature.spending.ui.BalanceBreakdown
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import java.sql.SQLException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpendingInteractor @Inject
constructor(private val spendingsRepository: SpendingsRepository,
            private val schedulerProvider: SchedulerProvider,
            monzoInteractor: MonzoInteractor) {

    val spendingSubject = PublishSubject.create<Lce<List<Spending>>>()

    init {
        monzoInteractor.subscribeToMonzoSpendings({
            when {
                it.loading -> spendingSubject.onNext(it)
                it.hasError() -> spendingSubject.onNext(it)
                else -> it.content?.let(this::createOrUpdateMonzoCategories)
            }
        }, {
            spendingSubject.onNext(Lce.error(it))
        })
    }

    // REACTIVE METHODS

    @SuppressLint("CheckResult")
    fun loadSpendings() {
        spendingsRepository.getAll()
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.mainThread())
            .doOnSubscribe {
                spendingSubject.onNext(Lce.loading())
            }
            .subscribe({
                spendingSubject.onNext(Lce.content(it))
            }, { throwable ->
                spendingSubject.onNext(Lce.error(DomainException("Error loading from database. See logs.", throwable)))
            })
    }

    @SuppressLint("CheckResult")
    fun clearSpendings() {
        spendingsRepository.deleteAll()
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.mainThread())
            .doOnSubscribe {
                spendingSubject.onNext(Lce.loading())
            }
            .subscribe(this::loadSpendings, { throwable ->
                if (throwable is SQLException) {
                    spendingSubject.onNext(Lce.error(throwable))
                }
            })
    }

    @SuppressLint("CheckResult")
    private fun createOrUpdateMonzoCategories(spendings: List<Spending>) {
        spendingsRepository.createOrUpdateSpendings(spendings, MONZO_CATEGORY)
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.mainThread())
            .subscribe(this::loadSpendings, { throwable ->
                spendingSubject.onNext(Lce.error(DomainException("Error saving monzo spendings. See logs.", throwable)))
            })
    }

    fun deleteSpentByCycleData(spending: Spending) {
        spendingsRepository.deleteCycleSpendingBySpending(spending)
    }

    // NON-REACTIVE METHODS

    fun createOrUpdate(spending: Spending): Completable {
        return spendingsRepository.createOrUpdate(spending)
            .onErrorResumeNext { throwable ->
                Completable.error(DomainException("Error updating spending in database. See logs.", throwable))
            }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.mainThread())
    }

    fun delete(id: Int): Completable {
        return spendingsRepository.delete(id)
            .onErrorResumeNext {
                Completable.error(DomainException("Error deleting spending $id in database. See logs.", it))
            }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.mainThread())
    }

    fun get(id: Int): Maybe<Spending> {
        return spendingsRepository.get(id)
            .onErrorResumeNext { throwable: Throwable ->
                Maybe.error<Spending>(DomainException("Error reading spending $id from database. See logs.", throwable))
            }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.mainThread())
    }

    fun getByRemoteCategory(category: String, remoteCategoryKey: String): Observable<Spending> {
        return spendingsRepository.getSpendingByRemoteCategory(category, remoteCategoryKey)
            .onErrorResumeNext { throwable: Throwable ->
                Observable.error<Spending>(DomainException("Error reading spending with category `$category` from database. See logs.", throwable))
            }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.mainThread())
    }

    fun getBalance(): Single<Balance> {
        return spendingsRepository.getBalance()
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.mainThread())
    }

    fun getBalanceBreakdown(): BalanceBreakdown {
        return spendingsRepository.getBalanceBreakdown()
    }

    fun getTargetSavingBreakdown(): String {
        return spendingsRepository.getSavingsBreakdown()
    }

    fun getBalanceBreakdownCategoryDetails(category: ApiSpending.Category): String? {
        return spendingsRepository.getBalanceBreakdownCategoryDetails(category)
    }
}
