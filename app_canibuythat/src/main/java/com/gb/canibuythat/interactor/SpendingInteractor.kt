package com.gb.canibuythat.interactor

import com.gb.canibuythat.exception.DomainException
import com.gb.canibuythat.model.Balance
import com.gb.canibuythat.db.model.ApiSpending
import com.gb.canibuythat.model.Spending
import com.gb.canibuythat.repository.SpendingsRepository
import com.gb.canibuythat.rx.SchedulerProvider
import com.j256.ormlite.dao.Dao
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.sql.SQLException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpendingInteractor @Inject
constructor(private val repo: SpendingsRepository,
            private val schedulerProvider: SchedulerProvider) {

    private val spendingsSubject: Subject<Lce<List<Spending>>> = PublishSubject.create<Lce<List<Spending>>>()

    fun getSpendingsDataStream(): Subject<Lce<List<Spending>>> {
        return spendingsSubject
    }

    // REACTIVE METHODS

    fun loadSpendings() {
        repo.all
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
                .doOnSubscribe {
                    spendingsSubject.onNext(Lce.loading())
                }
                .subscribe({
                    spendingsSubject.onNext(Lce.content(it))
                }, { throwable ->
                    spendingsSubject.onNext(Lce.error(DomainException("Error loading from database. See logs.", throwable)))
                })
    }

    fun clearSpendings() {
        repo.deleteAll()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
                .doOnSubscribe {
                    spendingsSubject.onNext(Lce.loading())
                }
                .subscribe({
                    loadSpendings()
                }, { throwable ->
                    if (throwable is SQLException) {
                        spendingsSubject.onNext(Lce.error(throwable))
                    }
                })
    }

    fun createOrUpdateMonzoCategories(spendings: List<Spending>) {
        repo.createOrUpdateMonzoSpendings(spendings)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
                .subscribe({
                    loadSpendings()
                }, { throwable ->
                    spendingsSubject.onNext(Lce.error(DomainException("Error updating monzo cache. See logs.", throwable)))
                })
    }

    // NON-REACTIVE METHODS

    fun getSpendingsWithTarget(): Single<Array<Spending>> {
        return repo.getSpendingsWithTarget()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun createOrUpdate(spending: Spending): Single<Dao.CreateOrUpdateStatus> {
        return repo.createOrUpdate(spending)
                .onErrorResumeNext { throwable -> Single.error<Dao.CreateOrUpdateStatus>(DomainException("Error updating spending in database. See logs.", throwable)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun delete(id: Int): Completable {
        return repo.delete(id)
                .onErrorResumeNext { Completable.error(DomainException("Error deleting spending $id in database. See logs.")) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun get(id: Int): Maybe<Spending> {
        return repo.get(id)
                .onErrorResumeNext { throwable: Throwable -> Maybe.error<Spending>(DomainException("Error reading spending $id from database. See logs.", throwable)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun getByMonzoCategory(category: String): Observable<Spending> {
        return repo.getSpendingByMonzoCategory(category)
                .onErrorResumeNext { throwable: Throwable -> Observable.error<Spending>(DomainException("Error reading spending with category `$category` from database. See logs.", throwable)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun getBalance(): Single<Balance> {
        return repo.getBalance()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun getBalanceBreakdown(): Array<Pair<ApiSpending.Category, String>> {
        return repo.getBalanceBreakdown()
    }

    fun getTargetBalanceBreakdown(): String {
        return repo.getTargetBalanceBreakdown()
    }

    fun getTargetSavingBreakdown(): String {
        return repo.getSavingsBreakdown()
    }

    fun getBalanceBreakdownCategoryDetails(category: ApiSpending.Category): String? {
        return repo.getBalanceBreakdownCategoryDetails(category)
    }
}
