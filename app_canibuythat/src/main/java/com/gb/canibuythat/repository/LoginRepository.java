package com.gb.canibuythat.repository;

import com.gb.canibuythat.MonzoConstants;
import com.gb.canibuythat.api.MonzoApi;
import com.gb.canibuythat.model.Login;

import javax.inject.Inject;

import io.reactivex.Single;

public class LoginRepository {

    private MonzoApi monzoApi;
    private LoginMapper mapper = new LoginMapper();

    @Inject
    public LoginRepository(MonzoApi monzoApi) {
        this.monzoApi = monzoApi;
    }

    public Single<Login> login(String authorizationCode) {
        return monzoApi.login("access_token", MonzoConstants.CLIENT_ID,
                MonzoConstants.CLIENT_SECRET, MonzoConstants.MONZO_URI_AUTH_CALLBACK, authorizationCode)
                .map(apiLogin -> mapper.map(apiLogin));
    }
}
