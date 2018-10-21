package com.gb.canibuyit.repository

import com.gb.canibuyit.api.MonzoDispatchApi
import com.gb.canibuyit.model.DispatchRegistration
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonzoDispatchRepository @Inject constructor(private val monzoDispatchApi: MonzoDispatchApi, private val dispatchMapper: MonzoDispatchMapper) {

    fun register(token: String): Single<DispatchRegistration> {
        return monzoDispatchApi.register(token).map { dispatchMapper.mapToDispatchRegistration(it) }
    }
}
