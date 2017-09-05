package com.gb.canibuythat.interactor

import android.content.Context
import com.gb.canibuythat.exception.DomainException
import com.gb.canibuythat.interactor.model.LceSpendings
import com.gb.canibuythat.model.Balance
import com.gb.canibuythat.model.Spending
import com.gb.canibuythat.provider.SpendingProvider
import com.gb.canibuythat.repository.SpendingsRepository
import com.gb.canibuythat.rx.SchedulerProvider
import com.j256.ormlite.dao.Dao
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpendingInteractor @Inject
constructor(private val spendingsRepository: SpendingsRepository, private val appContext: Context, private val schedulerProvider: SchedulerProvider) {

    val spendingsSubject: Subject<LceSpendings> = PublishSubject.create<LceSpendings>()

    /**
     * Don't forget to dispose when presenter is discarded
     */
    fun registerForSpendings(onNext: Consumer<List<Spending>>, onError: Consumer<Throwable>, onLoading: Consumer<Boolean>): Disposable {
        return spendingsSubject.subscribe({
            if (it.hasError()) {
                onError.accept(it.error!!)
            } else if (it.isLoading) {
                onLoading.accept(true)
            } else {
                onNext.accept(it.data!!)
            }
        })
    }

    fun loadSpendings() {
        spendingsRepository.all
                .onErrorResumeNext { throwable: Throwable -> Maybe.error<List<Spending>>(DomainException("Error loading from database. See logs.", throwable)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
                .doOnSubscribe {
                    spendingsSubject.onNext(LceSpendings(true))
                }
                .subscribe({
                    spendingsSubject.onNext(LceSpendings(it))
                }, { throwable ->
                    spendingsSubject.onNext(LceSpendings(throwable))
                })
    }

    fun clearSpendings() {
        spendingsRepository.deleteAll()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
                .doOnSubscribe {
                    spendingsSubject.onNext(LceSpendings(true))
                }
                .subscribe({
                    loadSpendings()
                }, { throwable ->
                    spendingsSubject.onNext(LceSpendings(throwable))
                })
    }

    fun createOrUpdate(spending: Spending): Single<Dao.CreateOrUpdateStatus> {
        return spendingsRepository.createOrUpdate(spending)
                .onErrorResumeNext { throwable -> Single.error<Dao.CreateOrUpdateStatus>(DomainException("Error updating spending in database. See logs.", throwable)) }
                .doOnSuccess { appContext.contentResolver.notifyChange(SpendingProvider.SPENDINGS_URI, null) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun createOrUpdateMonzoCategories(spendings: List<Spending>) {
        spendingsRepository.createOrUpdateMonzoCategories(spendings)
                .onErrorResumeNext { throwable -> Completable.error(DomainException("Error updating monzo cache. See logs.", throwable)) }
                .doOnComplete { appContext.contentResolver.notifyChange(SpendingProvider.SPENDINGS_URI, null) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
                .subscribe(this::loadSpendings, { spendingsSubject.onNext(LceSpendings(it)) })
    }

    fun delete(id: Int): Completable {
        return spendingsRepository.delete(id)
                .onErrorResumeNext { Completable.error(DomainException("Error deleting spending $id in database. See logs.")) }
                .doOnComplete { appContext.contentResolver.notifyChange(SpendingProvider.SPENDINGS_URI, null) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun read(id: Int): Maybe<Spending> {
        return spendingsRepository.read(id)
                .onErrorResumeNext { throwable: Throwable -> Maybe.error<Spending>(DomainException("Error reading spending $id from database. See logs.", throwable)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun calculateBalance(): Single<Balance> {
        return spendingsRepository.calculateBalance()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }
}
