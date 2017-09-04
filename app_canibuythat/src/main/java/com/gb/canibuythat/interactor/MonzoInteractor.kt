package com.gb.canibuythat.interactor

import com.gb.canibuythat.exception.MonzoException
import com.gb.canibuythat.model.Login
import com.gb.canibuythat.model.Spending
import com.gb.canibuythat.repository.MonzoRepository
import com.gb.canibuythat.rx.SchedulerProvider
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonzoInteractor @Inject
constructor(private val schedulerProvider: SchedulerProvider, private val monzoRepository: MonzoRepository, private val spendingInteractor: SpendingInteractor) {

    fun login(authorizationCode: String): Single<Login> {
        return monzoRepository.login(authorizationCode)
                .onErrorResumeNext { Single.error(MonzoException(it)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun refreshSession(refreshToken: String): Single<Login> {
        return monzoRepository.refreshSession(refreshToken)
                .onErrorResumeNext { Single.error(MonzoException(it)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun loadTransactions(accountId: String) {
        monzoRepository.getTransactions(accountId)
                .onErrorResumeNext { Single.error(MonzoException(it)) }
                .subscribeOn(schedulerProvider.io())
                .subscribe(spendingInteractor::createOrUpdateMonzoCategories, {
                    spendingInteractor.subject.onError(MonzoException(it))
                })
    }
}
