package com.gb.canibuythat.repository;

import com.gb.canibuythat.MonzoConstants;
import com.gb.canibuythat.api.BaseFormDataApi;
import com.gb.canibuythat.api.MonzoApi;
import com.gb.canibuythat.model.Account;
import com.gb.canibuythat.model.Login;
import com.gb.canibuythat.model.Transaction;

import javax.inject.Inject;

import io.reactivex.Single;

public class MonzoRepository extends BaseFormDataApi {

    private MonzoApi monzoApi;
    private MonzoMapper mapper = new MonzoMapper();

    @Inject
    public MonzoRepository(MonzoApi monzoApi) {
        this.monzoApi = monzoApi;
    }

    public Single<Login> login(String authorizationCode) {
        return monzoApi.login(text("authorization_code"),
                text(authorizationCode),
                text(MonzoConstants.MONZO_URI_AUTH_CALLBACK),
                text(MonzoConstants.CLIENT_ID),
                text(MonzoConstants.CLIENT_SECRET))
                .map(apiLogin -> mapper.map(apiLogin));
    }

    public Single<Login> refresh(String refreshToken) {
        return monzoApi.refresh(text("refresh_token"),
                text(refreshToken),
                text(MonzoConstants.CLIENT_ID),
                text(MonzoConstants.CLIENT_SECRET))
                .map(apiLogin -> mapper.map(apiLogin));
    }

    public Single<Account[]> accounts() {
        return monzoApi.accounts().map(apiAccountsResponse -> mapper.map(apiAccountsResponse));
    }

    public Single<Transaction[]> transactions(String accountId) {
        return monzoApi.transactions(accountId).map(apiTransactionCollection -> mapper.map(apiTransactionCollection));
    }
}
