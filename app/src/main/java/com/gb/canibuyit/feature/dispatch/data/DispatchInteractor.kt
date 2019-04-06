package com.gb.canibuyit.feature.dispatch.data

import com.gb.canibuyit.error.DomainException
import com.gb.canibuyit.feature.dispatch.model.DispatchRegistration
import com.gb.canibuyit.base.rx.SchedulerProvider
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DispatchInteractor @Inject
constructor(private val dispatchRepository: DispatchRepository,
            private val schedulerProvider: SchedulerProvider) {

    fun register(token: String): Single<DispatchRegistration> {
        return dispatchRepository.register(token)
                .onErrorResumeNext {
                    Single.error(
                            DomainException("Error registering for Monzo push notification", it))
                }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }
}