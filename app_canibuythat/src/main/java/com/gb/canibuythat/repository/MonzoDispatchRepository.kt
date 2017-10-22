package com.gb.canibuythat.repository

import com.gb.canibuythat.api.MonzoDispatchApi
import com.gb.canibuythat.model.DispatchRegistration
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonzoDispatchRepository @Inject constructor(private val monzoDispatchApi: MonzoDispatchApi, private val dispatchMapper: MonzoDispatchMapper) {

    fun register(token: String): Single<DispatchRegistration> {
        return monzoDispatchApi.register(token).map { dispatchMapper.mapToDispatchRegistration(it) }
    }
}
