package com.gb.canibuyit.interactor

import android.annotation.SuppressLint
import com.gb.canibuyit.db.model.ApiSpending
import com.gb.canibuyit.exception.DomainException
import com.gb.canibuyit.model.Balance
import com.gb.canibuyit.model.CycleSpent
import com.gb.canibuyit.model.Lce
import com.gb.canibuyit.model.Spending
import com.gb.canibuyit.model.SpentByCycleUpdateUiModel
import com.gb.canibuyit.repository.SpendingsRepository
import com.gb.canibuyit.rx.SchedulerProvider
import com.gb.canibuyit.ui.BalanceBreakdown
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
constructor(private val spendingsRepository: SpendingsRepository,
            private val schedulerProvider: SchedulerProvider) {

    private val spendingUIModel: Subject<Lce<List<Spending>>> = PublishSubject.create<Lce<List<Spending>>>()

    fun spendingUIModel(): Subject<Lce<List<Spending>>> = spendingUIModel

    // REACTIVE METHODS

    @SuppressLint("CheckResult")
    fun loadSpendings() {
        spendingsRepository.getAll()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
                .doOnSubscribe {
                    spendingUIModel.onNext(Lce.loading())
                }
                .subscribe({
                    spendingUIModel.onNext(Lce.content(it))
                }, { throwable ->
                    spendingUIModel.onNext(Lce.error(DomainException("Error loading from database. See logs.", throwable)))
                })
    }

    @SuppressLint("CheckResult")
    fun clearSpendings() {
        spendingsRepository.deleteAll()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
                .doOnSubscribe {
                    spendingUIModel.onNext(Lce.loading())
                }
                .subscribe({
                    loadSpendings()
                }, { throwable ->
                    if (throwable is SQLException) {
                        spendingUIModel.onNext(Lce.error(throwable))
                    }
                })
    }

    @SuppressLint("CheckResult")
    fun createOrUpdateMonzoCategories(spendings: List<Spending>) {
        spendingsRepository.createOrUpdateMonzoSpendings(spendings)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
                .subscribe({
                    loadSpendings()
                }, { throwable ->
                    spendingUIModel.onNext(Lce.error(DomainException("Error saving monzo spendings. See logs.", throwable)))
                })
    }

    fun setSpentByCycleEnabled(cycleSpent: CycleSpent, enabled: Boolean): Observable<SpentByCycleUpdateUiModel> {
        return spendingsRepository.setSpentByCycleEnabled(cycleSpent, enabled)
                .map { result -> this.mapSpentByCycleEnabledResult(cycleSpent, result) }
                .toObservable()
                .startWith(SpentByCycleUpdateUiModel.Loading(cycleSpent))
                .onErrorResumeNext { throwable: Throwable ->
                    Observable.just(SpentByCycleUpdateUiModel.Error(
                            cycleSpent = cycleSpent,
                            error = throwable.message ?: "Error updating SpentByCycle ($cycleSpent).")
                    )
                }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    private fun mapSpentByCycleEnabledResult(cycleSpent: CycleSpent, enabled: Boolean): SpentByCycleUpdateUiModel {
        return SpentByCycleUpdateUiModel.Success(cycleSpent.copy(enabled = enabled))
    }

    fun setAllSpentByCycleEnabled(spending: Spending, enabled: Boolean) {
        spending.spentByCycle?.let { list ->
            list.forEach { cycleSpent ->
                spendingsRepository.setSpentByCycleEnabled(cycleSpent, enabled)
            }
        }
    }

    fun deleteSpentByCycleData(spending: Spending) {
        spendingsRepository.deleteSpendByCycleBySpendingId(spending)
    }

    // NON-REACTIVE METHODS

    fun createOrUpdate(spending: Spending): Completable {
        return spendingsRepository.createOrUpdate(spending)
                .onErrorResumeNext { throwable -> Completable.error(DomainException("Error updating spending in database. See logs.", throwable)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun delete(id: Int): Completable {
        return spendingsRepository.delete(id)
                .onErrorResumeNext { Completable.error(DomainException("Error deleting spending $id in database. See logs.", it)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun get(id: Int): Maybe<Spending> {
        return spendingsRepository.get(id)
                .onErrorResumeNext { throwable: Throwable -> Maybe.error<Spending>(DomainException("Error reading spending $id from database. See logs.", throwable)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun getByMonzoCategory(category: String): Observable<Spending> {
        return spendingsRepository.getSpendingByMonzoCategory(category)
                .onErrorResumeNext { throwable: Throwable -> Observable.error<Spending>(DomainException("Error reading spending with category `$category` from database. See logs.", throwable)) }
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

    fun getTargetBalanceBreakdown(): String {
        return spendingsRepository.getTargetBalanceBreakdown()
    }

    fun getTargetSavingBreakdown(): String {
        return spendingsRepository.getSavingsBreakdown()
    }

    fun getBalanceBreakdownCategoryDetails(category: ApiSpending.Category): String? {
        return spendingsRepository.getBalanceBreakdownCategoryDetails(category)
    }
}
