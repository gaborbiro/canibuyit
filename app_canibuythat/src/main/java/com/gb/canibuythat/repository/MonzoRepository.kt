package com.gb.canibuythat.repository

import com.gb.canibuythat.MonzoConstants
import com.gb.canibuythat.api.BaseFormDataApi
import com.gb.canibuythat.api.MonzoApi
import com.gb.canibuythat.model.Login
import com.gb.canibuythat.model.Spending
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonzoRepository @Inject constructor(private val monzoApi: MonzoApi, private val mapper: MonzoMapper) : BaseFormDataApi() {

    fun login(authorizationCode: String): Single<Login> {
        return monzoApi.login(text("authorization_code"),
                text(authorizationCode),
                text(MonzoConstants.MONZO_URI_AUTH_CALLBACK),
                text(MonzoConstants.CLIENT_ID),
                text(MonzoConstants.CLIENT_SECRET))
                .map(mapper::mapToLogin)
    }

    fun refreshSession(refreshToken: String): Single<Login> {
        return monzoApi.refresh(text("refresh_token"),
                text(refreshToken),
                text(MonzoConstants.CLIENT_ID),
                text(MonzoConstants.CLIENT_SECRET))
                .map(mapper::mapToLogin)
    }

    fun getSpendings(accountId: String): Single<List<Spending>> {
        return monzoApi.transactions(accountId)
                .map(mapper::mapToTransactions)
                .map {
                    it.groupBy { it.category }.map {
                        val (category, transactionsForCategory) = it
                        mapper.mapToSpending(category, transactionsForCategory)
                    }
                }
    }
}
