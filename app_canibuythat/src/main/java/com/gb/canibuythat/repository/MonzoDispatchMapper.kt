package com.gb.canibuythat.repository

import com.gb.canibuythat.api.model.ApiDispatchRegistration
import com.gb.canibuythat.model.DispatchRegistration
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class MonzoDispatchMapper @Inject constructor() {

    fun mapToDispatchRegistration(apiDispatchRegistration: ApiDispatchRegistration): DispatchRegistration {
        return DispatchRegistration(apiDispatchRegistration.hash)
    }
}
