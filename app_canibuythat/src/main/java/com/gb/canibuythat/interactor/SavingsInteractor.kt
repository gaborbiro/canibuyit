package com.gb.canibuythat.interactor

import com.gb.canibuythat.model.Saving
import com.gb.canibuythat.repository.SavingsRepository
import com.gb.canibuythat.rx.SchedulerProvider
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject

class SavingsInteractor @Inject constructor(private val savingsRepository: SavingsRepository,
                                            private val schedulerProvider: SchedulerProvider) {
    fun save(savings: Array<Saving>): Completable {
        return savingsRepository.create(savings)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }

    fun getSavingsForSpending(spendingId: Int): Single<Array<Saving>> {
        return savingsRepository.getSavingsForSpending(spendingId)
    }

    fun getAll(): Array<Saving> {
        return savingsRepository.getAll()
    }

    fun clearAll(): Single<Int> {
        return savingsRepository.clearAll()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }
}