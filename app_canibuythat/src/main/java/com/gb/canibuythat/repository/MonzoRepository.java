package com.gb.canibuythat.repository;

import com.gb.canibuythat.MonzoConstants;
import com.gb.canibuythat.api.MonzoApi;
import com.gb.canibuythat.model.Account;
import com.gb.canibuythat.model.Login;

import javax.inject.Inject;

import io.reactivex.Single;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class MonzoRepository {

    private MonzoApi monzoApi;
    private MonzoMapper mapper = new MonzoMapper();

    @Inject
    public MonzoRepository(MonzoApi monzoApi) {
        this.monzoApi = monzoApi;
    }

    public Single<Login> login(String authorizationCode) {
        RequestBody grantTypeBody = RequestBody.create(MediaType.parse("text/plain"), "authorization_code");
        RequestBody clientIdBody = RequestBody.create(MediaType.parse("text/plain"), MonzoConstants.CLIENT_ID);
        RequestBody clientSecretBody = RequestBody.create(MediaType.parse("text/plain"), MonzoConstants.CLIENT_SECRET);
        RequestBody authorizationCodeBody = RequestBody.create(MediaType.parse("text/plain"), authorizationCode);
        RequestBody redirectUriBody = RequestBody.create(MediaType.parse("text/plain"), MonzoConstants.MONZO_URI_AUTH_CALLBACK);
        return monzoApi.login(grantTypeBody, authorizationCodeBody, redirectUriBody, clientIdBody, clientSecretBody).map(apiLogin -> mapper.map(apiLogin));
    }

    public Single<Login> refresh(String refreshToken) {
        RequestBody grantTypeBody = RequestBody.create(MediaType.parse("text/plain"), "refresh_token");
        RequestBody refreshTokenBody = RequestBody.create(MediaType.parse("text/plain"), refreshToken);
        RequestBody clientIdBody = RequestBody.create(MediaType.parse("text/plain"), MonzoConstants.CLIENT_ID);
        RequestBody clientSecretBody = RequestBody.create(MediaType.parse("text/plain"), MonzoConstants.CLIENT_SECRET);
        return monzoApi.refresh(grantTypeBody, refreshTokenBody, clientIdBody, clientSecretBody)
                .map(apiLogin -> mapper.map(apiLogin));
    }

    public Single<Account[]> accounts() {
        return monzoApi.accounts().map(apiAccountsResponse -> mapper.map(apiAccountsResponse));
    }
}
