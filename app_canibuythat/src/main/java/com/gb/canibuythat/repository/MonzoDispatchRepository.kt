package com.gb.canibuythat.repository

import com.gb.canibuythat.api.MonzoDispatchApi
import io.reactivex.Completable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonzoDispatchRepository @Inject constructor(private val monzoDispatchApi: MonzoDispatchApi) {

    fun register(name: String, token: String): Completable {
        return Completable.create {
            try {
                monzoDispatchApi.register(name, token)
                it.onComplete()
            } catch (t: Throwable) {
                it.onError(t)
            }
        }
    }
}
