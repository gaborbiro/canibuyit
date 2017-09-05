package com.gb.canibuythat.interactor

import com.gb.canibuythat.exception.MonzoException
import com.gb.canibuythat.interactor.model.LceLogin
import com.gb.canibuythat.interactor.model.LceSpendings
import com.gb.canibuythat.model.Login
import com.gb.canibuythat.repository.MonzoRepository
import com.gb.canibuythat.rx.SchedulerProvider
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonzoInteractor @Inject
constructor(private val schedulerProvider: SchedulerProvider, private val monzoRepository: MonzoRepository, private val spendingInteractor: SpendingInteractor) {

    private val loginSubject: Subject<LceLogin> = PublishSubject.create<LceLogin>()

    /**
     * Don't forget to dispose when presenter is discarded
     */
    fun registerForLogin(onNext: Consumer<Login>, onError: Consumer<Throwable>, onLoading: Consumer<Boolean>): Disposable {
        return loginSubject.subscribe({
            if (it.hasError()) {
                onError.accept(it.error!!)
            } else if (it.isLoading) {
                onLoading.accept(true)
            } else {
                onNext.accept(it.data!!)
            }
        })
    }

    fun login(authorizationCode: String) {
        monzoRepository.login(authorizationCode)
                .onErrorResumeNext { Single.error(MonzoException(it)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
                .doOnSubscribe {
                    loginSubject.onNext(LceLogin(true))
                }
                .subscribe({
                    loginSubject.onNext(LceLogin(it))
                }, { throwable ->
                    loginSubject.onNext(LceLogin(throwable))
                })
    }

    fun refreshSession(refreshToken: String) {
        monzoRepository.refreshSession(refreshToken)
                .onErrorResumeNext { Single.error(MonzoException(it)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun loadTransactions(accountId: String) {
        monzoRepository.getTransactions(accountId)
                .onErrorResumeNext { Single.error(MonzoException(it)) }
                .subscribeOn(schedulerProvider.io())
                .subscribe(spendingInteractor::createOrUpdateMonzoCategories, {
                    spendingInteractor.spendingsSubject.onNext(LceSpendings(MonzoException(it)))
                })
    }
}
