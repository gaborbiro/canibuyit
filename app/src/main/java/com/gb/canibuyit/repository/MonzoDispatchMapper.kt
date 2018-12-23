package com.gb.canibuyit.repository

import com.gb.canibuyit.api.model.ApiDispatchRegistration
import com.gb.canibuyit.model.DispatchRegistration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonzoDispatchMapper @Inject constructor() {

    fun mapToDispatchRegistration(apiDispatchRegistration: ApiDispatchRegistration): DispatchRegistration {
        return DispatchRegistration(apiDispatchRegistration.hash)
    }
}
