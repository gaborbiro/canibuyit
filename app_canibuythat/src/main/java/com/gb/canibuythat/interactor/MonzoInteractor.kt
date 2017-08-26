package com.gb.canibuythat.interactor

import com.gb.canibuythat.exception.MonzoException
import com.gb.canibuythat.model.Account
import com.gb.canibuythat.model.Login
import com.gb.canibuythat.model.Transaction
import com.gb.canibuythat.repository.MonzoRepository
import com.gb.canibuythat.rx.SchedulerProvider
import io.reactivex.Single
import javax.inject.Inject

class MonzoInteractor @Inject
constructor(private val schedulerProvider: SchedulerProvider, private val monzoRepository: MonzoRepository) {

    fun login(authorizationCode: String): Single<Login> {
        return monzoRepository.login(authorizationCode)
                .onErrorResumeNext { throwable -> Single.error<Login>(MonzoException(throwable)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun refresh(refreshToken: String): Single<Login> {
        return monzoRepository.refresh(refreshToken)
                .onErrorResumeNext { throwable -> Single.error<Login>(MonzoException(throwable)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun accounts(): Single<List<Account>> {
        return monzoRepository.accounts()
                .onErrorResumeNext { throwable -> Single.error<List<Account>>(MonzoException(throwable)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun transactions(accountId: String): Single<List<Transaction>> {
        return monzoRepository.transactions(accountId)
                .onErrorResumeNext { throwable -> Single.error<List<Transaction>>(MonzoException(throwable)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }
}
