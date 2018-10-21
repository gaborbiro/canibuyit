package com.gb.canibuyit.interactor

import com.gb.canibuyit.exception.DomainException
import com.gb.canibuyit.model.DispatchRegistration
import com.gb.canibuyit.repository.MonzoDispatchRepository
import com.gb.canibuyit.rx.SchedulerProvider
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonzoDispatchInteractor @Inject
constructor(private val monzoDispatchRepository: MonzoDispatchRepository, private val schedulerProvider: SchedulerProvider) {

    fun register(token: String): Single<DispatchRegistration> {
        return monzoDispatchRepository.register(token)
                .onErrorResumeNext { Single.error(DomainException("Error registering for Monzo push notification", it)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }
}