package com.gb.canibuythat.interactor

import com.gb.canibuythat.CredentialsProvider
import com.gb.canibuythat.exception.DomainException
import com.gb.canibuythat.exception.MonzoException
import com.gb.canibuythat.model.Login
import com.gb.canibuythat.model.Spending
import com.gb.canibuythat.repository.MonzoRepository
import com.gb.canibuythat.rx.SchedulerProvider
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonzoInteractor @Inject
constructor(private val schedulerProvider: SchedulerProvider,
            private val monzoRepository: MonzoRepository,
            private val spendingInteractor: SpendingInteractor,
            private val credentialsProvider: CredentialsProvider) {

    private val loginSubject: Subject<Lce<Login>> = PublishSubject.create<Lce<Login>>()

    fun getLoginDataStream(): Subject<Lce<Login>> {
        return loginSubject
    }

    fun login(authorizationCode: String): Disposable {
        return monzoRepository.login(authorizationCode)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
                .doOnSubscribe {
                    loginSubject.onNext(Lce.loading())
                }
                .subscribe({
                    loginSubject.onNext(Lce.content(it))
                }, { throwable ->
                    loginSubject.onNext(Lce.error(MonzoException(throwable)))
                })
    }

    private fun refreshSession(refreshToken: String): Single<Login> {
        return monzoRepository.refreshSession(refreshToken)
                .subscribeOn(schedulerProvider.io())
    }

    fun getSpendingsDataStream(): Observable<Lce<List<Spending>>> {
        return spendingInteractor.getSpendingsDataStream()
    }

    fun loadSpendings(accountId: String): Disposable {
        return monzoRepository.getSpendings(accountId)
                .subscribeOn(schedulerProvider.io())
                .doOnSubscribe {
                    spendingInteractor.getSpendingsDataStream().onNext(Lce.loading())
                }
                .subscribe(spendingInteractor::createOrUpdateMonzoCategories, { throwable ->
                    val e: MonzoException = MonzoException(throwable)
                    if (e.action == DomainException.Action.LOGIN && credentialsProvider.refreshToken != null) {
                        refreshSession(credentialsProvider.refreshToken!!)
                                .subscribe({
                                    loadSpendings(accountId)
                                }, {
                                    spendingInteractor.getSpendingsDataStream().onNext(Lce.error(MonzoException(it)))
                                })
                    } else {
                        spendingInteractor.getSpendingsDataStream().onNext(Lce.error(e))
                    }
                })
    }
}
