package com.gb.canibuyit.feature.dispatch.data

import com.gb.canibuyit.feature.dispatch.api.ApiDispatchRegistration
import com.gb.canibuyit.feature.dispatch.model.DispatchRegistration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DispatchMapper @Inject constructor() {

    fun mapToDispatchRegistration(apiDispatchRegistration: ApiDispatchRegistration): DispatchRegistration {
        return DispatchRegistration(apiDispatchRegistration.hash)
    }
}
