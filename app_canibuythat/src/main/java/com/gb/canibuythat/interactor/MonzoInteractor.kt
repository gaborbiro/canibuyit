package com.gb.canibuythat.interactor

import com.gb.canibuythat.exception.MonzoException
import com.gb.canibuythat.model.Account
import com.gb.canibuythat.model.Spending
import com.gb.canibuythat.model.Login
import com.gb.canibuythat.repository.MonzoRepository
import com.gb.canibuythat.rx.SchedulerProvider
import io.reactivex.Single
import javax.inject.Inject

class MonzoInteractor @Inject
constructor(private val schedulerProvider: SchedulerProvider, private val monzoRepository: MonzoRepository, private val spendingInteractor: SpendingInteractor) {

    fun login(authorizationCode: String): Single<Login> {
        return monzoRepository.login(authorizationCode)
                .onErrorResumeNext { throwable -> Single.error<Login>(MonzoException(throwable)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun refreshSession(refreshToken: String): Single<Login> {
        return monzoRepository.refreshSession(refreshToken)
                .onErrorResumeNext { throwable -> Single.error<Login>(MonzoException(throwable)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun getAccounts(): Single<List<Account>> {
        return monzoRepository.getAccounts()
                .onErrorResumeNext { throwable -> Single.error<List<Account>>(MonzoException(throwable)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun getSpendings(accountId: String): Single<List<Spending>> {
        return monzoRepository.getSpendings(accountId)
                .onErrorResumeNext { throwable -> Single.error<List<Spending>>(MonzoException(throwable)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }
}
