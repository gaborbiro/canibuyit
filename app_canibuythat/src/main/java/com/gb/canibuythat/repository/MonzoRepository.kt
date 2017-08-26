package com.gb.canibuythat.repository

import com.gb.canibuythat.MonzoConstants
import com.gb.canibuythat.api.BaseFormDataApi
import com.gb.canibuythat.api.MonzoApi
import com.gb.canibuythat.model.Account
import com.gb.canibuythat.model.Login
import com.gb.canibuythat.model.Transaction
import io.reactivex.Single
import javax.inject.Inject

class MonzoRepository @Inject
constructor(private val monzoApi: MonzoApi) : BaseFormDataApi() {
    private val mapper = MonzoMapper()

    fun login(authorizationCode: String): Single<Login> {
        return monzoApi.login(text("authorization_code"),
                text(authorizationCode),
                text(MonzoConstants.MONZO_URI_AUTH_CALLBACK),
                text(MonzoConstants.CLIENT_ID),
                text(MonzoConstants.CLIENT_SECRET))
                .map { apiLogin -> mapper.map(apiLogin) }
    }

    fun refresh(refreshToken: String): Single<Login> {
        return monzoApi.refresh(text("refresh_token"),
                text(refreshToken),
                text(MonzoConstants.CLIENT_ID),
                text(MonzoConstants.CLIENT_SECRET))
                .map { apiLogin -> mapper.map(apiLogin) }
    }

    fun accounts(): Single<List<Account>> {
        return monzoApi.accounts().map { apiAccountsResponse -> mapper.map(apiAccountsResponse) }
    }

    fun transactions(accountId: String): Single<List<Transaction>> {
        return monzoApi.transactions(accountId).map { apiTransactionCollection -> mapper.map(apiTransactionCollection) }
    }
}
