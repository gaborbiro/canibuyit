package com.gb.canibuyit.feature.dispatch.data

import com.gb.canibuyit.feature.dispatch.api.DispatchApi
import com.gb.canibuyit.feature.dispatch.model.DispatchRegistration
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DispatchRepository @Inject constructor(private val dispatchApi: DispatchApi,
                                             private val dispatchMapper: DispatchMapper) {

    fun register(token: String): Single<DispatchRegistration> {
        return dispatchApi.register(token).map { dispatchMapper.mapToDispatchRegistration(it) }
    }
}
