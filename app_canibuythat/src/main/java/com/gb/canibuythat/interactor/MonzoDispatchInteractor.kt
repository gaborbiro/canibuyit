package com.gb.canibuythat.interactor

import android.content.Context
import com.gb.canibuythat.exception.MonzoException
import com.gb.canibuythat.repository.MonzoDispatchRepository
import com.gb.canibuythat.rx.SchedulerProvider
import io.reactivex.Completable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonzoDispatchInteractor @Inject
constructor(private val monzoDispatchRepository: MonzoDispatchRepository, private val appContext: Context, private val schedulerProvider: SchedulerProvider) {

    fun register(name: String, token: String): Completable {
        return monzoDispatchRepository.register(name, token)
                .onErrorResumeNext { Completable.error(MonzoException(it)) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.mainThread())
    }
}